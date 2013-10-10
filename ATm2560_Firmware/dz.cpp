/* Speedoino - This file is part of the firmware.
 * Copyright (C) 2011 Kolja Windeler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


 /* timer usage by the speedoino codebase
  * Timer0 -  8 Bit - Used by the Arduino Base to calc the millis() timestamp
  * Timer1 - 16 Bit - Used by the RPM capturing module, normal mode, 2MHz
  * Timer2 -  8 Bit - Used by the Arduino Base to generate Phase correct PWM, 250kHz, Pins: PH6 (outer RGB), PB4 (not used)
  * Timer3 - 16 Bit - Used by the Aktor Module to switch the color-fade providing PWM values of the outer LEDs at the correct time
  * Timer4 - 16 Bit - Used by the Arduino Base to generate Phase correct PWM, 250kHz, Pins: PH3 (not used), PH4..5 (outer RGB)
  * Timer5 - 16 Bit - not in use now, could be used by the correct Wheel speed detection, OCM on "pulse per rotation" and MATCH ISR on trigger
  */

#include "global.h"

speedo_dz::speedo_dz(){
	exact=0;                 // real rotation speed
	blitz_dz=0;
	blitz_en=false;
	dz_faktor_counter=0;
}

unsigned int speedo_dz::get_dz(bool exact_dz){
	return exact;
}

ISR(INT4_vect){
	uint8_t sreg;
	uint16_t timerValue;
	sreg = SREG;		/* Save global interrupt flag */
	cli();				/* Disable interrupts */
	timerValue = TCNT1;	/* Read TCNTn into i */
	TCNT1=0;			/* Reset Timer value */
	SREG = sreg;		/* Restore global interrupt flag */
	// timer runs with 2 MHz
	// if the engine runs with 3.000 rpm we should have 3000/60*2(double ignition)=100 pulses per second
	// that leads to 10ms per Pulse. Timer1 value will be at 20.000 after 10ms.
	pSensors->m_dz->overruns=0;
	pSensors->m_dz->set_exact((uint32_t)60000000UL / (((uint32_t)pSensors->m_dz->overruns<<16) + timerValue));

//	uint16_t rpm=(uint32_t)60000000UL / (((uint32_t)pSensors->m_dz->overruns<<16) + timerValue);
//	uint16_t last_rpm=pSensors->m_dz->get_dz(true);
//	uint16_t differ=rpm-last_rpm;
//
//	pSensors->m_dz->set_exact(last_rpm+(differ&(~511))+((differ&511)>>4)); // division 12 cycles
}

// this overflow will occure after 65536/2000000=0,032768 sec, 0,032768=30/RPM, RPM=30/0,032768=915 min
ISR(TIMER1_OVF_vect){
	uint8_t sreg;
	sreg = SREG;	/* Save global interrupt flag */
	cli();			/* Disable interrupts */
	TCNT1=0;
	SREG = sreg;	/* Restore global interrupt flag */
	pSensors->m_dz->overruns++;
	if(pSensors->m_dz->overruns>4){ // no spark for 164ms -> less than 183 rpm
		pSensors->m_dz->set_exact(0);
	}
}

void speedo_dz::set_exact(uint16_t i){
	if(i<15000){
		exact=i;
	} else {
		exact=15000;
	}
}


void speedo_dz::init() {
	EIMSK |= (1<<INT4); // Enable Interrupt
	EICRB |= (1<<ISC40) | (1<<ISC41); // rising edge on INT4

	// timer
	TCNT1=0x00; // reset value
	TIMSK1=(1<<TOIE1); // <- activate Timer overflow interrupt
	TCCR1B=(0<<CS12) | (1<<CS11) | (0<<CS10); // selects clock to "/8" => 16Mhz/8=2MHz,
	TCCR1A=0; // WGM=0 -> normal mode

	pDebug->sprintlnp(PSTR("DZ init done"));
	blitz_en=false;
	overruns=0;
	Serial3.flush();
};

void speedo_dz::shutdown(){
	EIMSK &= ~(1<<INT4); // DISABLE Interrupt
	EICRB &= ~(1<<ISC40) | (1<<ISC41); // no edge on INT4
};


int speedo_dz::check_vars(){
	if(blitz_dz==0){
		pDebug->sprintp(PSTR("DZ failed"));
		blitz_dz=12500; // hornet maessig
		blitz_en=true; // gehen wir mal von "an" aus
		return 1;
	}
	return 0;
};
