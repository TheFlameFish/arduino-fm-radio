#include <TEA5767N.h>  // https://github.com/mroger/TEA5767
#include <Wire.h>

TEA5767N radio = TEA5767N();

float frequency = 89.9;

void setup() {
  Serial.begin(9600);
  Serial.println("Starting");

  radio.setMonoReception();
  radio.setStereoNoiseCancellingOn();
  radio.selectFrequency(frequency);
}

void loop() {
  if (Serial.available() > 0) {
    String input = Serial.readStringUntil('\n');  // Read the input as a string
    input.trim();  // Trim any surrounding whitespace or newlines

    // If input is not empty
    if (input.length() > 0) {
      frequency = input.toFloat();  // Convert string to float

      if (frequency > 0.0) {  // Valid frequency
        radio.turnTheSoundBackOn();
        radio.setStandByOff();
        radio.selectFrequency(frequency);
        Serial.println(frequency);
      } else {  // If frequency is zero or invalid, mute the radio
        radio.mute();
        radio.setStandByOn();
        Serial.println("Muted");
      }
    }
  }
}
