// Controller for Boxjoint jig (Roger Xue)
// 
// - it reads comma separated int as thou of movements from serial port
// - it has home and limit switch on both end
// - when not homed or limit is triggered, moves can't be executed
// - as soon as limit is tripped, jig will stop moving, even in a manual move state.
// - manual move overrides limit switch, so you can get out of tripped state,
//   also means you can damage the jig by manual move it.
// 

#include <Wire.h>
// Get the LCD I2C Library here: 
// https://bitbucket.org/fmalpartida/new-liquidcrystal/downloads
#include <LiquidCrystal_I2C.h>

// stepsPerRevolution * microstep * TPI / 1000
#define STEP_PER_THOU (200 * 8 * 18 / 1000)

#define MAX_MOVES 200

#define HOME_SWITCH 2 // interrupt pin
#define LIMIT_SWITCH 3 // interrupt pin

#define GO_HOME 4
#define MOVE_LEFT 6
#define MOVE_RIGHT 5
#define EXECUTE 7

#define PUL 8
#define DIR 9

#define SPEED_POT A0

// set the LCD address to 0x27 for a 16 chars 2 line display
// A FEW use address 0x3F
// Set the pins on the I2C chip used for LCD connections:
//                    addr, en,rw,rs,d4,d5,d6,d7,bl,blpol
LiquidCrystal_I2C lcd(0x27, 2, 1, 0, 4, 5, 6, 7, 3, POSITIVE);  // Set the LCD I2C address

unsigned int moves[MAX_MOVES];
unsigned int moveCount(0);
unsigned int currentMove(0);
bool leftReleasedAfterLimitTripped(false);
bool rightReleasedAfterLimitTripped(false);

// volatile value needed if changed in interrupt function.
volatile bool homed(false);
volatile bool tripped(false);
// in steps
long position(0);

// in ns
int stepDelay(10);

void setup() {
  Serial.begin(9600);

  // LCD setup
  lcd.begin(20, 4);
  lcd.setCursor(0, 0);
  lcd.print("        Boxjoint jig");
  lcd.setCursor(0, 1);
  lcd.print("           Roger Xue");
  
  // pin setup
  pinMode(GO_HOME, INPUT);
  digitalWrite(GO_HOME, INPUT_PULLUP);
  pinMode(HOME_SWITCH, INPUT);
  digitalWrite(HOME_SWITCH, INPUT_PULLUP);
  pinMode(MOVE_LEFT, INPUT);
  digitalWrite(MOVE_LEFT, INPUT_PULLUP);
  pinMode(MOVE_RIGHT, INPUT);
  digitalWrite(MOVE_RIGHT, INPUT_PULLUP);
  pinMode(LIMIT_SWITCH, INPUT);
  digitalWrite(LIMIT_SWITCH, INPUT_PULLUP);
  pinMode(EXECUTE, INPUT);
  digitalWrite(EXECUTE, INPUT_PULLUP);
  
  pinMode(PUL, OUTPUT);
  pinMode(DIR, OUTPUT);
  digitalWrite(PUL, LOW);
  digitalWrite(DIR, LOW);
  
  attachInterrupt(digitalPinToInterrupt(HOME_SWITCH), homeTriggered, CHANGE);
//  attachInterrupt(digitalPinToInterrupt(LIMIT_SWITCH), limitTriggered, CHANGE);
}

void loop() {
  if (goHomePressed()) {
    goHome();
  }
  if (moveRightPressed()) {
    moveRight();
  }
  if (moveLeftPressed()) {
    moveLeft();
  }
  if (!digitalRead(EXECUTE)) {
    executeMoves();
  }
  updateSpeedPot();
  readSerial();
}

inline void updateSpeedPot() {
  int thisStepDelay = map(analogRead(SPEED_POT), 0, 1023, 45, 400);
  if (abs(thisStepDelay - stepDelay) > 4) {
    stepDelay = thisStepDelay;
    showStatus();
  } else {
    return;
  }
}

inline void moveRight() {
  while ((!tripped || rightReleasedAfterLimitTripped) && moveRightPressed()) {
    moveIt(1);
  }
  showStatus();
}

inline void moveLeft() {
  while ((!tripped || leftReleasedAfterLimitTripped) && moveLeftPressed()) {
    moveIt(-1);
  }
  showStatus();
}

inline void goHome() {
  homed = false;
  // manual can override 
  while (!homed && !tripped && !moveLeftPressed() && !moveRightPressed()) {
    moveIt(-1);
  }
  delay(10);
  // back off untill limit switch is nolonger tripped.
  // manual can override 
  while (tripped && !moveLeftPressed() && !moveRightPressed()) {
    moveIt(1);
  }
  position = 0;
  currentMove = 0;
  showStatus();
}

// interrupt function
void homeTriggered() {
  if (digitalRead(HOME_SWITCH)) {
    homed = true;
    tripped = true;
  } else {
    tripped = false;
  }
}

// interrupt function
void limitTriggered() {
  tripped = digitalRead(LIMIT_SWITCH) ? true : false;
}

inline void executeMoves() {
  if (moveCount == 0) {
    clearLcdLine(0);
    lcd.setCursor(0, 0);
    lcd.print("No moves to execute.");
    blinkLcd();
    return;
  } else if (tripped) {
    clearLcdLine(0);
    lcd.setCursor(0, 0);
    lcd.print("Limit tripped.");
    blinkLcd();
    return;
  } else if (!homed) {
    clearLcdLine(0);
    lcd.setCursor(0, 0);
    lcd.print("Not Homed.");
    blinkLcd();
    return;
  } else if (currentMove >= moveCount) {
    clearLcdLine(0);
    lcd.setCursor(0, 0);
    lcd.print("All moves done.");
    blinkLcd();
  } else {
    int thisMove = moves[currentMove++];
    move(thisMove);
    // make sure no double trigger.
    if (thisMove < 100) {
      delay(300);
    }
  }
}

inline void move(int thou) {
    int steps = abs(thou) * STEP_PER_THOU;
    int movedSteps(0);
    clearLcdLine(1);
    lcd.setCursor(0, 1);
    lcd.print("moving ");
    lcd.print(steps);
    while (!tripped && steps > movedSteps++) {
      moveIt(thou);
    }
    if (tripped) {
      blinkLcd();
    }
    clearLcdLine(1);
    lcd.setCursor(0, 1);
    lcd.print("moved ");
    lcd.print(movedSteps);
    showStatus();
}

inline void moveIt(int direction) {
  digitalWrite(DIR, direction > 0 ? HIGH : LOW);
  direction > 0 ? position++ : position--;
  digitalWrite(PUL, HIGH);
  delayMicroseconds(stepDelay);
  digitalWrite(PUL, LOW);
  delayMicroseconds(stepDelay);
}

inline void readSerial() {
  if (Serial.available()) {
    clearLcdLine(0);
    lcd.setCursor(0, 0);
    lcd.print("Downloading....");
    moveCount = 0;
    currentMove = 0;
    while (Serial.available() > 0 && moveCount < MAX_MOVES) {
        moves[moveCount++] = Serial.parseInt();
    }
    if (moveCount >= MAX_MOVES) {
      clearLcdLine(0);
      lcd.setCursor(0, 0);
      lcd.print("Exceeds max moves.");
      blinkLcd();
    } else {
      clearLcdLine(0);
      lcd.setCursor(0, 0);
      lcd.print(moveCount);
      lcd.print(" downloaded");
    }
  }
}


inline bool moveLeftPressed() {
  bool pressed = !digitalRead(MOVE_LEFT);
  if (!pressed && tripped) {
    leftReleasedAfterLimitTripped = true;
  } else if (pressed && !tripped) {
    leftReleasedAfterLimitTripped = false;
  }
  return pressed;
}

inline bool moveRightPressed() {
  bool pressed = !digitalRead(MOVE_RIGHT);
  if (!pressed && tripped) {
    rightReleasedAfterLimitTripped = true;
  } else if (pressed && !tripped) {
    rightReleasedAfterLimitTripped = false;
  }
  return pressed;
}

inline bool goHomePressed() {
  return !digitalRead(GO_HOME);
}

// * means it's not homed yet.
inline void showStatus() {
  clearLcdLine(2);
  lcd.setCursor(0, 2);
  if (!homed) {
    lcd.print("POS*:");
  } else {
    lcd.print("POS:");
  }
  lcd.print(position / STEP_PER_THOU);

  clearLcdLine(3);
  lcd.setCursor(0, 3);
  lcd.print("SPD:");
  lcd.print(stepDelay);
  lcd.print(" tr:");
  lcd.print(tripped);
  lcd.print(" home:");
  lcd.print(homed);
}

inline void clearLcdLine(int line) {
  lcd.setCursor(0, line);
  lcd.print("                    ");
}
  
void blinkLcd() {
  for(int i = 0; i < 3; ++i) {
    lcd.backlight();
    delay(150);
    lcd.noBacklight();
    delay(150);
  }
  lcd.backlight();
}

