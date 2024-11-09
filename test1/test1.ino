#include <HX711.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <SoftwareSerial.h>
#include <pitches.h>

#define BT_RXD				  2
#define BT_TXD				  3
#define Buzzer				  4
#define LOADCELL_DOUT_PIN	  5
#define LOADCELL_SCK_PIN	  6

#define LCD_Address			  0x27
#define BT_BUFFER_SIZE		  128

#define ROAD_CELL_CALIBRATION -23000  // 로드셀 캘리브레이션
#define LCD_ON_WEIGHT		  10.0	  // 사람 올라갔을때 LCD 화면 켜지는 임계값 무게, 단위 kg
#define LCD_OFF_WEIGHT		  3.0	  // 사람 내려갔을때 LCD 화면 꺼지는 임계값 무게, 단위 kg
#define WELCOME_SCREEN_DELAY  10000	  // 부팅하고 웰컴스크린 표시시간, 단위 ms
#define WELCOME_MESSAGE_DELAY 200	  // 부팅 메시지 표시시간, 단위 ms
#define MAIN_LOOP_INTERVAL	  10	  // 메인 무한루프문 반복시간, 단위 ms
#define LCD_OFF_TIME		  3000	  // 사람 내려가고 LCD가 꺼질 때까지의 시간, 단위 ms

// 징글징글한년 멜로디와 음길이
const int melody[]		  = {NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_G5, NOTE_C5, NOTE_D5, NOTE_E5, NOTE_F5, NOTE_F5, NOTE_F5, NOTE_F5, NOTE_F5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_D5, NOTE_D5, NOTE_E5, NOTE_D5, NOTE_G5};
const int MusicDuration[] = {8, 8, 4, 8, 8, 4, 8, 8, 8, 8, 2, 8, 8, 8, 8, 8, 8, 8, 16, 16, 8, 8, 8, 8, 4, 4};

// 현재 기기 상태 명세
struct MachineState {
	bool FirstBootEn;  // 첫 부팅 여부
					   // bool DisplayActive;	 // 현재 디스플레이 활성화 여부
};

// 현재 기기 상태
MachineState State = {
	true,  // FirstBootEn
		   // false,	// display active
};

// 타이머 명세
struct Timer {
	unsigned long LoopTmr;			  // 메인 무한루프문 반복타이머
	unsigned long LcdOffTmr;		  // LCD 화면 꺼지는 타이머
	unsigned long LcdWelcomeLoading;  // LCD 웰컴스크린 왼쪽으로 한칸씩 이동 간격
	unsigned long LcdWelcomeTmr;	  // LCD 웰컴스크린 타이머
};

// 커스텀 글자1 (8개까지 가능)
byte Custom1[8] = {
	B01110,
	B01110,
	B01110,
	B10100,
	B11111,
	B00101,
	B11100,
	B10111,
};

// 현재 타이머
Timer Timer = {0, 0, 0, 0};

SoftwareSerial	  bluetooth(BT_RXD, BT_TXD);  // 블루투스 객체.
LiquidCrystal_I2C lcd(LCD_Address, 16, 2);	  // lcd 객체 선언, 가로 16칸, 세로 2칸
HX711			  Weight;					  // 로드셀 앰프 객체.

void Main_Function(void);

double RawWeight = 0;  // 로드셀 출력 생 무게

void setup() {
	// serial
	Serial.begin(115200);

	// pinmode
	pinMode(Buzzer, OUTPUT);

	// 블루투스
	bluetooth.begin(9600);
	// btRxBuffer.init();	// 블루투스 링버퍼 초기화

	// lcd
	lcd.begin();				 // LCD 사용 시작
	lcd.backlight();			 // 백라이트 켜기
	lcd.setCursor(0, 0);		 // 커서 초기화
	lcd.createChar(1, Custom1);	 // 커스텀 글자 1번을 생성 후 LCD에 등록(8개까지 가능)

	// 로드셀
	Weight.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);
	Weight.set_scale(ROAD_CELL_CALIBRATION);
	Weight.tare();
}

void loop() { Main_Function(); }

// early return 쓰기위해 루프함수 따로만듬
void Main_Function(void) {
	// 100ms 마다 루프실행
	if (millis() - Timer.LoopTmr <= MAIN_LOOP_INTERVAL) return;
	Timer.LoopTmr = millis();

	// 첫번째 부팅이면 웰컴화면, 로드셀 캘리브레이션 실행
	if (State.FirstBootEn) {
		First_Boot_Sequence();	// 5초동안 실행됨
		return;					// 웰컴화면 보고 종료됨
	}

	// Communication_Func();  // 블루투스, 시리얼 통신
	// Process_Bluetooth_Data();
	if (bluetooth.available()) {
		char c = bluetooth.read();
		if (c == '\n') return;	// 엔터키 무시
		Serial.print("블루투스 수신 데이터: ");
		Serial.println(c);
		// bluetooth.write("제 무게는 ");	// 답장
		bluetooth.println(RawWeight);
		// bluetooth.println("kg입니다.");
	}
	Get_Weight();	   // 로드셀 무게측정
	Handle_Display();  // 디스플레이 관리
}

// LCD 디스플레이 관리
void Handle_Display(void) {
	if (RawWeight > LCD_ON_WEIGHT) {  // 디스플레이 비활성화 되어있는데 10키로 이상 감지되면
		Activate_Display();			  // 디스플레이 활성화.
		Print_Weight();				  // 무게 출력
		Timer.LcdOffTmr = millis();

		Serial.println(Timer.LcdOffTmr);
	} else if (RawWeight < LCD_OFF_WEIGHT) {			  // 지금 무게가 디스플레이 꺼짐 무게보다 낮으면
		if (millis() - Timer.LcdOffTmr > LCD_OFF_TIME) {  // 무게가 3키로보다 낮은상태로 3초 이상 유지되면
			Deactivate_Display();						  // 화면 끄기
		} else {
			Print_Weight();
			Activate_Display();
		}
	} else {
		Activate_Display();
		Print_Weight();
		Timer.LcdOffTmr = millis();
	}
}

// 디스플레이 활성화
void Activate_Display() {
	if (!lcd.getBacklight()) {
		lcd.backlight();  // 백라이트 켬
	}
}

// 화면 끄기
void Deactivate_Display() {
	lcd.setCursor(0, 0);  // 커서 초기화
	lcd.clear();		  // 글자 지움
	lcd.noBacklight();	  // 백라이트 끔
}

// 무게 출력
void Print_Weight(void) {
	lcd.setCursor(0, 0);  // 첫번째줄
	lcd.print("Current Weight: ");

	lcd.setCursor(0, 1);  // 두번째줄
	lcd.print(RawWeight);
	lcd.setCursor(6, 1);  // 두번째줄
	lcd.print("kg");
	lcd.write(byte(1));
}

// 블루투스, 시리얼 통신
// void Communication_Func(void) {
// 	Process_Bluetooth_Data();  // 블루투스로 수신한 값에 대한 처리
// 	if (Serial.available()) {  // 시리얼 처리
// 							   // bluetooth.write(Serial.read());
// 	}
// }

// 블루투스 데이터 수신 인터럽트
// void serialEvent() {
// 	while (bluetooth.available()) {
// 		uint8_t receivedByte = bluetooth.read();
// 		btRxBuffer.write(receivedByte);
// 		Serial.print(receivedByte);
// 		Serial.println("ㅅㅂ블루투스 받았따.");
// 	}
// }

// 블루투스 수신된 데이터 처리
// void Process_Bluetooth_Data() {
// 	uint8_t receivedByte;

// 	while (btRxBuffer.read(&receivedByte)) {
// 		// 수신된 데이터 처리
// 		// 여기는 승필이랑 얘기해서 프로토콜 맞춰야됨
// 		switch (receivedByte) {
// 			case 'T':  // 영점
// 				Weight.tare();
// 				bluetooth.println("Zeroing complete");
// 				break;

// 			case 'W':  // 현재 무게 요청
// 				bluetooth.println(RawWeight);
// 				break;

// 			default:
// 				Serial.println("좆됐다ㅋㅋ 승필이가 이상한거 보내는데");
// 				break;
// 		}
// 	}
// }

// 무게 가져옴
bool Get_Weight() {
	if (Weight.is_ready()) {
		RawWeight = Weight.get_units();
		return true;
	}
	return false;
}

// 첫 부팅시 웰컴화면 출력 및 로드셀 캘리브레이션 여유 시간 준다
void First_Boot_Sequence(void) {
	static bool		   firstBoot	  = false;
	static int		   LcdCursor	  = 0;
	static const float ProgressTiming = WELCOME_SCREEN_DELAY / 16 - ((WELCOME_SCREEN_DELAY / 16) * 0.1);  // 웰컴스크린 시간 / 16칸 - ((웰컴스크린 시간/16칸) * 10%)

	// 이 함수 처음 들어왔을때만 실행
	if (!firstBoot) {
		Timer.LcdWelcomeTmr = Timer.LcdWelcomeLoading = millis();
		firstBoot									  = true;
		lcd.setCursor(0, 0);
		lcd.print("Welcome Loading");
		lcd.setCursor(0, 1);
		lcd.print("________________");
	}

	if (millis() - Timer.LcdWelcomeLoading > ProgressTiming) {	// 한칸씩 로딩바 나옴
		Timer.LcdWelcomeLoading = millis();
		lcd.setCursor(LcdCursor++, 1);
		lcd.print("0");
	}

	if (millis() - Timer.LcdWelcomeTmr > WELCOME_SCREEN_DELAY) {  // 웰컴스크린 5초 지나면
		State.FirstBootEn = false;								  // 웰컴스크린 실행 비트 해제
	}
}

// 징글벨 때리는 함수(주의: 노래 재생되는 동안 다른 기능 전부 중지됨)
void Music_Start(void) {
	static const int size = sizeof(MusicDuration) / sizeof(int);

	for (int note = 0; note < size; note++) {
		int duration = 1000 / MusicDuration[note];
		tone(Buzzer, melody[note], duration);

		int pauseBetweenNotes = duration * 1.30;
		delay(pauseBetweenNotes);

		noTone(Buzzer);
	}
}
