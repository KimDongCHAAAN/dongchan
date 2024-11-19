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

#define SEC					  1000
#define SECS(x)				  (SEC * x)

#define ROAD_CELL_CALIBRATION -23000	// 로드셀 캘리브레이션
#define LCD_ON_WEIGHT		  10.0		// 사람 올라갔을때 LCD 화면 켜지는 임계값 무게, 단위 kg
#define LCD_OFF_WEIGHT		  3.0		// 사람 내려갔을때 LCD 화면 꺼지는 임계값 무게, 단위 kg
#define WELCOME_SCREEN_DELAY  SECS(10)	// 부팅하고 웰컴스크린 표시시간, 단위 ms
#define WELCOME_MESSAGE_DELAY 200		// 부팅 메시지 표시시간, 단위 ms
#define MAIN_LOOP_INTERVAL	  10		// 메인 무한루프문 반복시간, 단위 ms
#define LCD_OFF_TIME		  SECS(3)	// 사람 내려가고 LCD가 꺼질 때까지의 시간, 단위 ms
#define ALARM_OFF_WEIGHT	  50.0		// 알람 활성화 시 알람 해제하기 위해서 넘겨야 하는 무게 임계값. 단위 kg

// 징글징글한년 멜로디와 음길이
const int melody[]		  = {NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_G5, NOTE_C5, NOTE_D5, NOTE_E5, NOTE_F5, NOTE_F5, NOTE_F5, NOTE_F5, NOTE_F5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_D5, NOTE_D5, NOTE_E5, NOTE_D5, NOTE_G5};
const int MusicDuration[] = {8, 8, 4, 8, 8, 4, 8, 8, 8, 8, 2, 8, 8, 8, 8, 8, 8, 8, 16, 16, 8, 8, 8, 8, 4, 4};

// 현재 기기 상태 명세
struct MachineState {
	bool FirstBootEn;  // 첫 부팅 여부
	bool AlarmActive;  // 알람 활성화 여부
					   // bool DisplayActive;	 // 현재 디스플레이 활성화 여부
};

// 현재 기기 상태
MachineState State = {
	true,  // FirstBootEn
	false  // AlarmActive active
};

// 타이머 명세
struct Timer {
	unsigned long LoopTmr;			  // 메인 무한루프문 반복타이머
	unsigned long LcdOffTmr;		  // LCD 화면 꺼지는 타이머
	unsigned long LcdWelcomeLoading;  // LCD 웰컴스크린 왼쪽으로 한칸씩 이동 간격
	unsigned long LcdWelcomeTmr;	  // LCD 웰컴스크린 타이머
	unsigned long AlarmWeightChkTmr;  // 알람 울려서 무게 올라갔을때 임계값 넘는 디바운스 타이머
	unsigned long AlarmOffTmr;		  // 알람 울려서 무게 올라갔을때 알람 꺼지기까지 타이머
	unsigned long MusicDelayTmr;	  // 음악 음 간 간격 타이머.
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

// 커스텀글자2
byte Custom2[8] = {
	B11111,
	B11111,
	B11111,
	B11111,
	B11111,
	B11111,
	B11111,
	B11111,
};

// 커스텀글자3
byte Custom3[8] = {
	B11111,
	B10001,
	B10001,
	B10001,
	B10001,
	B10001,
	B10001,
	B11111,
};
// 현재 타이머
Timer			  Timer = {0, 0, 0, 0, 0, 0, 0};
SoftwareSerial	  bluetooth(BT_RXD, BT_TXD);  // 블루투스 객체.
LiquidCrystal_I2C lcd(LCD_Address, 16, 2);	  // lcd 객체 선언, 가로 16칸, 세로 2칸
HX711			  Weight;					  // 로드셀 앰프 객체.

void Main_Function(void);

double RawWeight = 0;  // 로드셀 출력 생 무게

void setup() {
	// serial
	Serial.begin(115200);  // 이건 디버깅 전용 아두이노 내부 시리얼

	// pinmode
	pinMode(Buzzer, OUTPUT);

	// 블루투스
	bluetooth.begin(9600);	// 블루투스는 보드레이트 9600

	// lcd
	lcd.begin();				 // LCD 사용 시작
	lcd.backlight();			 // 백라이트 켜기
	lcd.setCursor(0, 0);		 // 커서 초기화

	lcd.createChar(1, Custom1);	 // 커스텀 글자 1번을 생성 후 LCD에 등록(8개까지 가능)
	lcd.createChar(2, Custom2);	 // 커스텀 글자 2번을 생성 후 LCD에 등록(8개까지 가능)
	lcd.createChar(3, Custom3);	 // 커스텀 글자 3번을 생성 후 LCD에 등록(8개까지 가능)

	// 로드셀
	Weight.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);	// 로드셀 객체 만든다
	Weight.set_scale(ROAD_CELL_CALIBRATION);			// 캘리브레이션 시작.
	Weight.tare();										// 부팅시 영점 잡음.
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

	Communication_Func();			  // 블루투스, 시리얼 통신

	Get_Weight();					  // 로드셀 무게측정
	Handle_Display();				  // 디스플레이 관리

	if (State.AlarmActive) {		  // 알람 비트 활성화되면.
		Handle_Alarm_Disable_Func();  // 사용자 무게 확인 및 무게로 인한 알람 비활성화 시 블루투스로 D 보냄.
	}
	Play_Music();					  // 음악 재생.
}

// 사용자 무게 확인 및 무게로 인한 알람 비활성화 시 블루투스로 D 보냄.
void Handle_Alarm_Disable_Func(void) {
	if (RawWeight > ALARM_OFF_WEIGHT) {					   // 현재 체중이 임계값 무게 넘으면
		if (millis() - Timer.AlarmWeightChkTmr > SEC) {	   // 그 상태로 1초 간 유지되면 그때부터 3초간 유지되는지 계산(노이즈 방지목적)
			if (millis() - Timer.AlarmOffTmr > SECS(3)) {  // 3초 이상 유지되면
				State.AlarmActive = false;				   // 알람 비트 해제
				bluetooth.println("D");					   // 블루투스로 알람 비트 해제 전송
			}
			Timer.AlarmOffTmr = millis();
		}
	} else {
		Timer.AlarmWeightChkTmr = millis();	 // 노이즈 방지(디바운스) 타이머 초기화
		Timer.AlarmOffTmr		= millis();	 // 3초 타이머 초기화.
	}
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
void Communication_Func(void) {
	// if (Serial.available()) {  // 시리얼 처리
	// 						   // bluetooth.write(Serial.read());
	// }

	if (bluetooth.available()) {
		char c = bluetooth.read();
		if (c == '\n') return;	// 엔터는 무시

		switch (c) {
			case 'W':  // 무게요청
				bluetooth.println(RawWeight);
				break;
			case 'A':						// 알람 활성화 요청 받으면
				State.AlarmActive = true;	// 알람 비트 활성화.
				bluetooth.println("A");		// 답장.
				break;
			default:						// 다른거 받았을때
				Serial.println("좆됐다ㅋㅋ 승필이가 이상한거 보내는데");
				bluetooth.println("Shit");	// 답장.
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
	static bool		   firstBoot	  = false;
	static int		   LcdCursor	  = 0;
	static const float ProgressTiming = (WELCOME_SCREEN_DELAY >> 4) - ((WELCOME_SCREEN_DELAY >> 4) * 0.1);	// 웰컴스크린 시간 / 16칸 - ((웰컴스크린 시간/16칸) * 10%) //여기ㅋㅋ

	// 이 함수 처음 들어왔을때만 실행
	if (!firstBoot) {
		Timer.LcdWelcomeTmr = Timer.LcdWelcomeLoading = millis();
		firstBoot									  = true;
		lcd.setCursor(0, 0);
		lcd.print("Welcome Loading");

		lcd.setCursor(0, 1);
		// 셋커서 한칸식 옮기고 write로 커스텀 빈칸 쓰기
		for (size_t i = 0; i < 16; i++) {
			lcd.setCursor(i, 1);
			lcd.write(byte(3));
		}
	}

	if (millis() - Timer.LcdWelcomeLoading > ProgressTiming) {	// 한칸씩 로딩바 나옴
		Timer.LcdWelcomeLoading = millis();
		lcd.setCursor(LcdCursor++, 1);
		lcd.write(byte(2));	 // 커스텀글자 커서 값 가져와서 출력
							 // lcd.print("0");
	}

	if (millis() - Timer.LcdWelcomeTmr > WELCOME_SCREEN_DELAY) {  // 웰컴스크린 5초 지나면
		State.FirstBootEn = false;								  // 웰컴스크린 실행 비트 해제
	}
}

// 징글벨 때리는 함수 (State.MusicActive의 상태 여부에 따라 음악 재생 여부가 갈림)
void Play_Music(void) {
	static const int size		   = sizeof(MusicDuration) / sizeof(int);  // 음악 전체 길이
	static int		 currentNote   = 0;									   // 현재 음 순서
	static bool		 isNotePlaying = false;								   // 지금 음 재생중인지 잠깐 쉬는박자인지

	if (!State.AlarmActive) {											   // 알람 비트가 비활성화되어있으면
		currentNote	  = 0;												   // 음악 재생되던 음 순서 0으로 되감기
		isNotePlaying = false;											   // 음 재생부터 시작하도록(쉬는 박자부터 시작하지 않도록)
		noTone(Buzzer);													   // 음 안나게
		return;															   // 종료
	}

	if (isNotePlaying) {																   // 잠깐 쉬는 박자 타이밍이면
		if (millis() - Timer.MusicDelayTmr > (SEC / MusicDuration[currentNote]) * 1.30) {  // 해당 박자 길이만큼 계산해서
			noTone(Buzzer);																   // 잠깐 소리 안내고 쉬기
			isNotePlaying = false;														   // 지금 소리 안낸다고 표시
			currentNote	  = (currentNote + 1) % size;									   // 음악 재생 끝나면 자동 되감기(무한 반복 재생)
		}
	} else {																			   // 지금 소리내야될 타이밍이면
		tone(Buzzer, melody[currentNote], SEC / MusicDuration[currentNote]);			   // 악보길이만큼 소리냄
		Timer.MusicDelayTmr = millis();													   // 음 쉬는 타이밍 타이머 초기화.
		isNotePlaying		= true;														   // 지금 소리 내고있다고 표시.
	}
}