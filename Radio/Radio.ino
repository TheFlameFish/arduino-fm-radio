#include <TEA5767N.h>  //https://github.com/mroger/TEA5767
#include <Wire.h>

TEA5767N radio = TEA5767N();

float frequency = 89.9;

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);

  Serial.println("Starting");

  // radio.mute();
  radio.setMonoReception();
  radio.setStereoNoiseCancellingOn();
  radio.selectFrequency(frequency);
}

void loop() {
  if (Serial.available() > 0) {
    float input = Serial.parseFloat();

    // If input is not zero and not invalid
    if (input != 0.0) {
      frequency = input;
      radio.turnTheSoundBackOn();
      radio.setStandByOff();
      radio.selectFrequency(frequency);
      Serial.println(frequency);
    } else {
      // Check if the input is truly zero or if it was a timeout/error
      if (Serial.peek() == '\n') {  // Check if the end of the input was reached
        radio.mute();
        radio.setStandByOn();
        Serial.println("Muted");
      }
    }
  }
}

