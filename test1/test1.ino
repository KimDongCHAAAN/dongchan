#include <HX711.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <SoftwareSerial.h>
// testtest
#define NOTE_B0 31
#define NOTE_C1 33
#define NOTE_CS1 35
#define NOTE_D1 37
#define NOTE_DS1 39
#define NOTE_E1 41
#define NOTE_F1 44
#define NOTE_FS1 46
#define NOTE_G1 49
#define NOTE_GS1 52
#define NOTE_A1 55
#define NOTE_AS1 58
#define NOTE_B1 62
#define NOTE_C2 65
#define NOTE_CS2 69
#define NOTE_D2 73
#define NOTE_DS2 78
#define NOTE_E2 82
#define NOTE_F2 87
#define NOTE_FS2 93
#define NOTE_G2 98
#define NOTE_GS2 104
#define NOTE_A2 110
#define NOTE_AS2 117
#define NOTE_B2 123
#define NOTE_C3 131
#define NOTE_CS3 139
#define NOTE_DB3 139
#define NOTE_D3 147
#define NOTE_DS3 156
#define NOTE_EB3 156
#define NOTE_E3 165
#define NOTE_F3 175
#define NOTE_FS3 185
#define NOTE_G3 196
#define NOTE_GS3 208
#define NOTE_A3 220
#define NOTE_AS3 233
#define NOTE_B3 247
#define NOTE_C4 262
#define NOTE_CS4 277
#define NOTE_D4 294
#define NOTE_DS4 311
#define NOTE_E4 330
#define NOTE_F4 349
#define NOTE_FS4 370
#define NOTE_G4 392
#define NOTE_GS4 415
#define NOTE_A4 440
#define NOTE_AS4 466
#define NOTE_B4 494
#define NOTE_C5 523
#define NOTE_CS5 554
#define NOTE_D5 587
#define NOTE_DS5 622
#define NOTE_E5 659
#define NOTE_F5 698
#define NOTE_FS5 740
#define NOTE_G5 784
#define NOTE_GS5 831
#define NOTE_A5 880
#define NOTE_AS5 932
#define NOTE_B5 988
#define NOTE_C6 1047
#define NOTE_CS6 1109
#define NOTE_D6 1175
#define NOTE_DS6 1245
#define NOTE_E6 1319
#define NOTE_F6 1397
#define NOTE_FS6 1480
#define NOTE_G6 1568
#define NOTE_GS6 1661
#define NOTE_A6 1760
#define NOTE_AS6 1865
#define NOTE_B6 1976
#define NOTE_C7 2093
#define NOTE_CS7 2217
#define NOTE_D7 2349
#define NOTE_DS7 2489
#define NOTE_E7 2637
#define NOTE_F7 2794
#define NOTE_FS7 2960
#define NOTE_G7 3136
#define NOTE_GS7 3322
#define NOTE_A7 3520
#define NOTE_AS7 3729
#define NOTE_B7 3951
#define NOTE_C8 4186
#define NOTE_CS8 4435
#define NOTE_D8 4699
#define NOTE_DS8 4978
#define REST 0

#define RoadCell0 0
#define RoadCell1 1
#define RoadCell2 2
#define RoadCell3 3
#define Buzzer 4
#define BT_RXD 0
#define BT_TXD 1
#define LCD_Address 0x27
#define LOADCELL_DOUT_PIN 2
#define LOADCELL_SCK_PIN 3

SoftwareSerial bluetooth(BT_RXD, BT_TXD);  // 블루투스 객체.
LiquidCrystal_I2C lcd(LCD_Address, 16, 2); // lcd 객체 선언, 가로 16칸, 세로 2칸
HX711 Weight;                              // 로드셀 앰프 객체.

int PrintTmr = 0;                    // 시리얼 프린트 타이머
float RoadCell_Calibration = -23000; // 로드셀 캘리브레이션
double RoadCell_Raw_Weight = 0;      // 로드셀 로우 무게

const int melody[] = {
    NOTE_E5, NOTE_E5, NOTE_E5,
    NOTE_E5, NOTE_E5, NOTE_E5,
    NOTE_E5, NOTE_G5, NOTE_C5, NOTE_D5,
    NOTE_E5,
    NOTE_F5, NOTE_F5, NOTE_F5, NOTE_F5,
    NOTE_F5, NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5,
    NOTE_E5, NOTE_D5, NOTE_D5, NOTE_E5,
    NOTE_D5, NOTE_G5};

const int durations[] = {
    8, 8, 4,
    8, 8, 4,
    8, 8, 8, 8,
    2,
    8, 8, 8, 8,
    8, 8, 8, 16, 16,
    8, 8, 8, 8,
    4, 4};

void setup()
{
    // serial
    Serial.begin(115200);

    // pinmode
    pinMode(Buzzer, OUTPUT);

    // 블루투스
    bluetooth.begin(9600);

    // lcd
    lcd.begin(); // LCD 사용 시작

    // 로드셀
    Weight.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);
    Weight.set_scale(RoadCell_Calibration);
    Weight.tare();
}

void loop()
{
    // if (bluetooth.available())
    // {
    //     Serial.write(bluetooth.read());
    // }
    // if (Serial.available())
    // {
    //     // bluetooth.write(Serial.read());
    // }

    // 100ms 마다 실행
    if (millis() - PrintTmr > 100)
    {
        PrintTmr = millis();

        // 100ms 마다 로드셀 계산 결과 확인되면 값 읽어옴
        if (Weight.is_ready())
        {
            RoadCell_Raw_Weight = Weight.get_units();
        }

        lcd.setCursor(0, 0); // 커서를 0, 0에 가져다 놓아라. (열, 행)
        lcd.print(RoadCell_Raw_Weight);
        Serial.println(RoadCell_Raw_Weight);

        lcd.setCursor(0, 1); // 커서를 0, 0에 가져다 놓아라. (열, 행)
        lcd.print("Shit ArthurDchan");

        // 1키로 넘으면 노래발사
        if (RoadCell_Raw_Weight > 1.0) // 1.0 -> 1키로
        {
            int size = sizeof(durations) / sizeof(int);

            for (int note = 0; note < size; note++)
            {
                int duration = 1000 / durations[note];
                tone(Buzzer, melody[note], duration);

                int pauseBetweenNotes = duration * 1.30;
                delay(pauseBetweenNotes);

                noTone(Buzzer);
            }
        }
        else
        {
            noTone(Buzzer);
        }

        // lcd.clear(); // 글자를 모두 지워라.
    }
}
