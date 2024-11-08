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
#define BOOT_SEQUENCE_DELAY	  5000	  // 부팅하고 웰컴스크린 표시시간, 단위 ms
#define MAIN_LOOP_INTERVAL	  100	  // 메인 무한루프문 반복시간, 단위 ms
#define LCD_OFF_TIME		  5000	  // 사람 내려가고 LCD가 꺼질 때까지의 시간, 단위 ms

// 링버퍼 구조체, 검색해보고 대충 개념이라도 파악하면 좋음
struct BTReceiveBuffer {
	uint8_t			 buffer[BT_BUFFER_SIZE];
	volatile uint8_t writeIndex;
	volatile uint8_t readIndex;
	volatile uint8_t dataCount;

	// 버퍼 초기화
	void init() {
		writeIndex = 0;
		readIndex  = 0;
		dataCount  = 0;
	}

	// 버퍼에 푸시년마냥 데이터 하나 삽입
	bool write(uint8_t data) {
		if (dataCount >= BT_BUFFER_SIZE) {
			return false;  // 버퍼 가득 참
		}

		buffer[writeIndex] = data;
		writeIndex		   = (writeIndex + 1) % BT_BUFFER_SIZE;
		dataCount++;
		return true;
	}

	// 버퍼에서 가장 마지막 데이터 하나 가져옴
	bool read(uint8_t* data) {
		if (dataCount == 0) {
			return false;  // 버퍼 비어있음
		}

		*data	  = buffer[readIndex];
		readIndex = (readIndex + 1) % BT_BUFFER_SIZE;
		dataCount--;
		return true;
	}

	// 지금 버퍼에 데이터가 있는지 확인
	uint8_t available() {
		return dataCount;
	}
};

// 징글징글한년 멜로디와 음길이
const int melody[]		  = {NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_G5, NOTE_C5, NOTE_D5, NOTE_E5, NOTE_F5, NOTE_F5, NOTE_F5, NOTE_F5, NOTE_F5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_D5, NOTE_D5, NOTE_E5, NOTE_D5, NOTE_G5};
const int MusicDuration[] = {8, 8, 4, 8, 8, 4, 8, 8, 8, 8, 2, 8, 8, 8, 8, 8, 8, 8, 16, 16, 8, 8, 8, 8, 4, 4};

// 현재 기기 상태 명세
typedef struct MachineState {
	bool FirstBootEn;	 // 첫 부팅 여부
	bool DisplayActive;	 // 현재 디스플레이 활성화 여부
};

// 현재 기기 상태
MachineState State = {
	true,	// FirstBootEn
	false,	// display active
};

// 타이머 명세
typedef struct Timer {
	bool LoopTmr;	 // 메인 무한루프문 반복타이머
	bool LcdOffTmr;	 // LCD 화면 꺼지는 타이머
};

// 현재 타이머
Timer Timer = {0};

SoftwareSerial	  bluetooth(BT_RXD, BT_TXD);  // 블루투스 객체.
LiquidCrystal_I2C lcd(LCD_Address, 16, 2);	  // lcd 객체 선언, 가로 16칸, 세로 2칸
HX711			  Weight;					  // 로드셀 앰프 객체.
BTReceiveBuffer	  btRxBuffer;				  // 블루투스 수신 링버퍼

double RawWeight = 0;						  // 로드셀 출력 생 무게

void setup() {
	// serial
	Serial.begin(115200);

	// pinmode
	pinMode(Buzzer, OUTPUT);

	// 블루투스
	bluetooth.begin(9600);
	btRxBuffer.init();	// 블루투스 링버퍼 초기화
	attachInterrupt(digitalPinToInterrupt(BT_RXD), NULL, FALLING);

	// lcd
	lcd.begin();		  // LCD 사용 시작
	lcd.autoscroll();	  // auto scroll 시작
	lcd.backlight();	  // 백라이트 끄기
	lcd.setCursor(0, 0);  // 커서 초기화
	lcd.noCursor();

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
		First_Boot_Sequence;  // 5초동안 실행됨
		return;				  // 웰컴화면 보고 종료됨
	}

	Communication_Func();  // 블루투스, 시리얼 통신
	Get_Weight();		   // 로드셀 무게측정
	Handle_Display();	   // 디스플레이 관리
}

// LCD 디스플레이 관리
void Handle_Display(void) {
	if (!State.DisplayActive && RawWeight > LCD_ON_WEIGHT) {  // 디스플레이 비활성화 되어있는데 10키로 이상 감지되면
		Activate_Display();									  // 디스플레이 활성화.
		Print_Weight();										  // 무게 출력
	} else if (State.DisplayActive) {						  // 디스플레이 활성화 되어있으면
		Print_Weight();										  // 무게 출력

		if (RawWeight < LCD_OFF_WEIGHT) {					  // 지금 무게가 디스플레이 꺼짐 무게보다 낮으면
			if (millis() - Timer.LcdOffTmr > LCD_OFF_TIME) {  // 무게가 낮은상태로 3초 이상 유지되면
				Deactivate_Display();						  // 화면 끄기
			}
		} else {											  // 지금 무게가 디스플레이 꺼짐 무게보다 높으면
			Timer.LcdOffTmr = millis();						  // 디스플레이 꺼짐 타이머 초기화
		}
	}
}

// 디스플레이 활성화
void Activate_Display() {
	State.DisplayActive = true;	 // 화면 상태 켬
	lcd.display();				 // 디스플레이 켜기
	lcd.backlight();			 // 백라이트 켬
}

// 화면 끄기
void Deactivate_Display() {
	State.DisplayActive = false;  // 화면 상태 끔
	lcd.setCursor(0, 0);		  // 커서 초기화
	lcd.clear();				  // 글자 지움
	lcd.noBacklight();			  // 백라이트 끔
	lcd.noDisplay();			  // 디스플레이 끄기
}

// 무게 출력
void Print_Weight(void) {
	lcd.setCursor(0, 0);  // 첫번째줄
	lcd.print("Current Weight: ");

	lcd.setCursor(0, 1);  // 두번째줄
	lcd.print(RawWeight);
	lcd.print("\t\tkg");

	// Serial.println(RawWeight);
}

// 블루투스, 시리얼 통신
void Communication_Func(void) {
	Process_Bluetooth_Data();  // 블루투스로 수신한 값에 대한 처리
	if (Serial.available()) {  // 시리얼 처리
							   // bluetooth.write(Serial.read());
	}
}

// 블루투스 데이터 수신 인터럽트
void serialEvent() {
	while (bluetooth.available()) {
		uint8_t receivedByte = bluetooth.read();
		btRxBuffer.write(receivedByte);
	}
}

// 블루투스 수신된 데이터 처리
void Process_Bluetooth_Data() {
	uint8_t receivedByte;

	while (btRxBuffer.read(&receivedByte)) {
		// 수신된 데이터 처리
		// 여기는 승필이랑 얘기해서 프로토콜 맞춰야됨
		switch (receivedByte) {
			case 'T':  // 영점
				Weight.tare();
				bluetooth.println("Zeroing complete");
				break;

			case 'W':  // 현재 무게 요청
				bluetooth.println(RawWeight);
				break;

			default:
				Serial.println("좆됐다ㅋㅋ 승필이가 이상한거 보내는데");
				break;
		}
	}
}

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
	lcd.setCursor(0, 0);
	lcd.print("Welcome");
	lcd.setCursor(0, 1);
	lcd.print("RoadCell Initializing...");

	if (millis() > BOOT_SEQUENCE_DELAY) {
		State.FirstBootEn = false;
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
