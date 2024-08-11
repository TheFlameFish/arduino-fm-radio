#include <TEA5767N.h>  // https://github.com/mroger/TEA5767
#include <Wire.h>

TEA5767N radio = TEA5767N();

float frequency = 89.9;

void setup() {
  Serial.begin(9600);

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
      } else if (input == "search") {
        search();
      } else {  // If frequency is zero or invalid, mute the radio
        radio.mute();
        radio.setStandByOn();
      }
    }
  }
}

void search() {
  byte isBandLimitReached = 0;

  radio.setSearchMidStopLevel();
  isBandLimitReached = radio.startsSearchMutingFromBeginning();
  

  //delay(250);
  
  String freq = String(radio.readFrequencyInMHz());
  printStation(freq);
  //delay(250);
  
  while (!isBandLimitReached) {
    
    //If you want listen to station search, use radio.searchNext() instead
    isBandLimitReached = radio.searchNext();
    

    //delay(250);
    
    freq = String(radio.readFrequencyInMHz());
    printStation(freq);
    //delay(250);
  }
  
  
  radio.setSearchDown();
  radio.setSearchMidStopLevel();
  
  isBandLimitReached = radio.searchNextMuting();
  

  //delay(250);
  
  freq = String(radio.readFrequencyInMHz());
  printStation(freq);
  //delay(250);
  
  while (!isBandLimitReached) {
    
    isBandLimitReached = radio.searchNext();
    
    //delay(250);
        
    freq = String(radio.readFrequencyInMHz());
    printStation(freq);
    //delay(250);
  }
}

void printStation(String freq) {
  if (radio.getSignalLevel() >= 9) {
    Serial.println(freq);
  }
}