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

#include "global.h"

speedo_speed::speedo_speed(){
	last_time=millis();
	reed_speed=0;
	speed_peaks=1; // avoid speed at startup
	reifen_umfang=0; // Reifenumfang in metern
	gps_takeover=0; // bei 120 km/h nehmen wir die Daten vom GPS statt des Reed wenn moeglich
	flat_counter_calibrate_umfang=0;
	flat_value_calibrate_umfang=2.00;

	// erstmal alle zwischenspeicher loeschen
	for(unsigned int i=0; i<sizeof(pSpeedo->max_speed)/sizeof(pSpeedo->max_speed[0]); i++){
		pSpeedo->trip_dist[i]=0;
		pSpeedo->avg_timebase[i]=0;
		pSpeedo->max_speed[i]=-99;
	};
};
speedo_speed::~speedo_speed(){
};

void speedo_speed::calc(){ // TODO: an stelle des prevent => if(digitalRead(3)==HIGH){ // außen drum herum
	unsigned long jetzt=millis();
	unsigned long differ=jetzt-last_time;
	status=SPEED_REED_OK;
	if(differ>250){ // mit max 10 hz und nach peak anzahl neu berechnen
		int temp_reed_speed=round(((reifen_umfang*1000*speed_peaks)/differ) * 3.6 ); // 1 Magnet // eventuell /360*280 => wenn 280° zwischen dem letzen und dem ersten flattern liegen, aber ich denke das werden eher 358° sein

#ifdef SPEED_DEBUG
		pDebug->sprintp(PSTR("Differ > 250 : "));
		Serial.print(differ);
		pDebug->sprintp(PSTR(" speed_peaks: "));
		Serial.print(speed_peaks);
		pDebug->sprintp(PSTR(" reifen umfang: "));
		Serial.print(reifen_umfang);
		pDebug->sprintp(PSTR(" temp_reed_speed: "));
		Serial.println(temp_reed_speed);
#endif

		if(temp_reed_speed<350 && temp_reed_speed>=0){
			reed_speed=temp_reed_speed;
		}
		last_time=jetzt;
		prevent_double_count=jetzt; // wir bekommen immer 2 peaks zur gleichen millis(), daher diese prevent_double_count
		speed_peaks=1;
	} else if((jetzt-prevent_double_count)>15) { // bis zu 470 km/h
		speed_peaks++;
		prevent_double_count=jetzt; // wir bekommen immer 2 peaks zur gleichen millis(), daher diese prevent_double_count
	};
};


ISR(INT6_vect){
	pSensors->m_speed->calc();
} // der eingentliche

void speedo_speed::init (){
	//DDRE  &=~(1<<SPEED_PIN); // interrupt 6 eingang
	last_time=millis(); //prevent calculation of rpm
	EIMSK |= (1<<INT6); // Enable Interrupt
	EICRB |= (1<<ISC60) | (1<<ISC61); // rising edge on INT5
	speed_peaks=0; // clear

	status=SPEED_REED_OK; // alles gut
	pDebug->sprintlnp(PSTR("Speed init done"));
};

void speedo_speed::shutdown(){
	EIMSK &= ~(1<<INT6); // DISABLE Interrupt
	EICRB &= ~((1<<ISC60) | (1<<ISC61)); // no edge on INT5
}


int speedo_speed::check_vars(){
	/********** check values ********************
	 * zumindest in den trip, max und avg nachsehen ob werte
	 * drinstehen. Falls die sd karte nicht gelesen werden konnte
	 * wird das schon gemeldet, aber falls nur die datei fuer trips
	 * nicht gelesen werden kann sollte man das hier merken weil
	 * alle(!) max value=0 sind wie von der initialisierung
	 ********** check values ********************/

	int nulls=0;
	for(unsigned int i=2; i<sizeof(pSpeedo->max_speed)/sizeof(pSpeedo->max_speed[0]); i++){
		if(pSpeedo->max_speed[i]==-99){
			nulls++;
		}
	};

	if((nulls == (sizeof(pSpeedo->max_speed)/sizeof(pSpeedo->max_speed[0]))-2) || reifen_umfang==0){
		reifen_umfang=1.99; // Reifenumfang in metern
		gps_takeover=120; // bei 120 km/h nehmen wir die Daten vom GPS statt des Reed wenn moeglich
		pDebug->sprintp(PSTR("speedo failed"));
		return 1;
	};

	return 0;
};


// für die gang berechnung ist der speed am magnet geiler als der vom gps weil beim beschleunigen sonst fehler kommen
int speedo_speed::get_mag_speed(){
	return reed_speed;
};

//void speedo_speed::check_umfang(){
//	int gps_speed=get_sat_speed();
//	int mag_speed=pSensors->get_speed(true);
//	if(pSensors->m_gps->get_info(6)<3 && pSpeedo->disp_zeile_bak[0]!=2){
//		pSpeedo->disp_zeile_bak[0]=2;
//		pOLED->highlight_bar(0,0,128,8);
//		pOLED->string_P(pSpeedo->default_font,PSTR("No GPS!"),5,0,15,0,0);
//		flat_counter_calibrate_umfang=0;
//	} else if(gps_speed<round(pSensors->m_speed->gps_takeover/0.6) && pSpeedo->disp_zeile_bak[0]!=1 && pSensors->m_gps->get_info(6)>=2){
//		pSpeedo->disp_zeile_bak[0]=1;
//		pOLED->highlight_bar(0,0,128,8);
//		pOLED->string_P(pSpeedo->default_font,PSTR("Speed up!"),5,0,15,0,0);
//		flat_counter_calibrate_umfang=0;
//	} else if(gps_speed>pSensors->m_speed->gps_takeover && pSpeedo->disp_zeile_bak[0]==1){
//		pOLED->filled_rect(0,0,128,8,0);
//		pSpeedo->disp_zeile_bak[0]=0;
//	}
//
//	if(gps_speed!=pSpeedo->disp_zeile_bak[1]){
//		pSpeedo->disp_zeile_bak[1]=gps_speed;
//		char speed_a[13];
//		sprintf(speed_a,"GPS %3i km/h",gps_speed);
//		pOLED->string(pSpeedo->default_font,speed_a,5,2);
//	}
//
//	if(mag_speed!=pSpeedo->disp_zeile_bak[2]){
//		pSpeedo->disp_zeile_bak[2]=mag_speed;
//		char speed_a[13];
//		sprintf(speed_a,"MAG %3i km/h",mag_speed);
//		pOLED->string(pSpeedo->default_font,speed_a,5,3);
//	}
//
//	//gps_geschwindigkeit/realer_reifenumfang = mag_speed/aktueller_umfang
//	//wenn ich eingestellt hätte das mein reifen nur 1m im umfang ist, dann wäre ich bei 196 km/h erst 98km/h schnell
//	// (196km/h) / (196cm/tick) = (98km/h) / (98cm/tick)
//	// also gps / ( mag / aktuell ) = realer umfang = gps * aktuell / mag
//	_delay_ms(150);
//	int new_length=(int)((unsigned long)(gps_speed*(int(pSensors->m_speed->reifen_umfang*100))/mag_speed));
//	flat_value_calibrate_umfang=pSensors->flatIt(new_length,&flat_counter_calibrate_umfang,64,flat_value_calibrate_umfang);
//
//	if(flat_value_calibrate_umfang!=pSpeedo->disp_zeile_bak[3]){
//		pSpeedo->disp_zeile_bak[2]=flat_value_calibrate_umfang;
//		char speed_a[13];
//		sprintf(speed_a,"outline now %3i cm",int(round(flat_value_calibrate_umfang)));
//		pOLED->string(pSpeedo->default_font,speed_a,1,6);
//	}
//
//	if(int(pSensors->m_speed->reifen_umfang*100)!=pSpeedo->disp_zeile_bak[4]){
//		pSpeedo->disp_zeile_bak[4]=int(pSensors->m_speed->reifen_umfang*100);
//		char speed_a[13];
//		sprintf(speed_a,"from file   %3i cm",int(pSensors->m_speed->reifen_umfang*100));
//		pOLED->string(pSpeedo->default_font,speed_a,1,7);
//	}
//}