#ifndef GLOBAL_H_
#define GLOBAL_H_

// sensoren
#include "clock_me.h"
#include "dz.h"
#include "gps.h"
#include "blinker.h"
#include "temperature.h"
#include "fuel.h"
#include "gear.h"
#include "speed.h"
#include "reset.h"
#include "oiler.h"

// sensoren
#include "display.h"
#include "sd.h"
#include "config.h" // vor timer
#include "timer.h" //nach config
#include "sprint.h"
#include "speedo.h"
#include "menu.h"
#include "sensors.h" // als letztes
#include "debug.h"
#include "file_manager.h"

/**********************************  working settings ********************************/
// development settings //
#define       GPS_SPEED_ONLY  false   // to ignore the magnetic value  -> hmm das ist doof, aber muss erstmal alleine arbeiten, mal sehen ob der gang angezeigt wird -> wenn ja => mag rennt
#define       BUTTONS_OFF     false   // disable all buttons
#define       DEMO_MODE       false   // shows dummy values
#define       WELCOME         true    // die frau am start
/**********************************  working settings ********************************/

#include <WProgram.h>
#include <util/delay.h> // timing
void setup();

#endif /* FKT_H_ */