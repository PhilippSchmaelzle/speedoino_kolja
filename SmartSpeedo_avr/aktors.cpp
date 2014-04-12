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

Speedo_aktors::Speedo_aktors(){
	m_oiler=new speedo_oiler();
//	bt_pin=1234;
};

Speedo_aktors::~Speedo_aktors(){
};


// run reset could be called in three ways,
// convertional by the bluetooth command
// -> that should write "Running Update", reconfigure serial speed and reset the ATm328
// second from the menu
// -> thereby only the "running update" should be written and the serial speed should be reconfigured
// third: from the state (2) if the user pushed "right" button to trigger reset
// -> only reset ATm328 and return to last state
void Speedo_aktors::run_reset_on_ATm328(char mode){
	// serial setup and "klickibunti"-"show some msg"
	if(mode==RESET_COMPLETE || mode==RESET_PREPARE){
		pSensors->m_reset->set_deactive(false,false);
		_delay_ms(100);// give serial 3 some time to send the messeage
		Serial3.end();
		Serial3.begin(115200);
//		pOLED->clear_screen();
//		pOLED->string_P(pSpeedo->default_font,PSTR("Running Update"),3,2);
//		pOLED->string_P(pSpeedo->default_font,PSTR("on AT328P"),5,3);
	};

	// show "_R_to_trigger_reset__" only if in "preparation" mode
	if(mode==RESET_PREPARE){
		char temp[2];
//		sprintf(temp,"%c",127);
//		pOLED->string(pSpeedo->default_font,temp,1,7);
//		pOLED->string_P(pSpeedo->default_font,PSTR("to trigger reset"),3,7);
	}

	// run the reset
	if(mode==RESET_COMPLETE || mode==RESET_KICK_TO_RESET){
		// pin as output
		DDRD |= (1<<ATM328RESETPIN);
		// set low -> low active
		PORTD &= ~(1<<ATM328RESETPIN);
		_delay_ms(50);
		PORTD |= (1<<ATM328RESETPIN);
		// set high, as pull up
		DDRD |= (1<<ATM328RESETPIN);
		PORTD |= (1<<ATM328RESETPIN);
	}

	// tunnel connection
	if(mode==RESET_COMPLETE || mode==RESET_PREPARE){
		// tunnel mode
		unsigned long timeout=millis();
		unsigned int max_time=5000; // should be enough if in android mode
		if(mode==RESET_PREPARE){ // longer timeout if in "menu-mode"
			max_time=30000;
		};
		while(millis()-timeout<max_time){ // time out
			while(Serial3.available()>0){
				Serial.print(Serial3.read(),BYTE);
				timeout=millis();
			}
			while(Serial.available()>0){
				Serial3.print(Serial.read(),BYTE);
				timeout=millis();
			}
			if(mode==RESET_PREPARE){
//				if(menu_state_on_enter==((pMenu->state/10))){ // button "right" was pushed
//					run_reset_on_ATm328(RESET_KICK_TO_RESET); // to the reset
//					pMenu->state=menu_state_on_enter; // reset menu state
//				} else if(menu_state_on_enter==((pMenu->state*10)+1)){ // button "left" was pushed
//					pMenu->state=menu_state_on_enter; // reset menu state, because menu->back will otherwise recaluclate two steps back
//					max_time=0;
//					break;
//				}
			}
		}
		Serial3.end();
		Serial3.begin(19200);
		pSensors->m_reset->set_active(false,false);
	};

}

void Speedo_aktors::init(){
	m_oiler->init();
	pDebug->sprintlnp(PSTR("Aktoren init ... Done"));
};


int Speedo_aktors::set_bt_pin(bool reset){
//	pOLED->clear_screen();
	char at_commands[22];
	bool connection_established=true;

	// check connection
//	pOLED->string_P(pSpeedo->default_font,PSTR("Checking connection"),0,0);

	sprintf(at_commands,"ATQ0%c",0x0D);
	if(ask_bt(&at_commands[0])!=0 || reset){											// fehler aufgetreten
		connection_established=false;
//		pOLED->string_P(pSpeedo->default_font,PSTR("FAILED"),14,1);								// hat nicht geklappt
		Serial.end();																	// setzte neue serielle Geschwindigkeit
		_delay_ms(500);
//		pOLED->string_P(pSpeedo->default_font,PSTR("TRYING 19200 BAUD"),0,2);					// hat nicht geklappt
		Serial.begin(19200);
		_delay_ms(2000);
		if(ask_bt(&at_commands[0])==0){
//			pOLED->string_P(pSpeedo->default_font,PSTR("OK"),14,3);
			_delay_ms(500);
//			pOLED->string_P(pSpeedo->default_font,PSTR("BASIC SETUP"),0,4);
			sprintf(at_commands,"ATE0%c",0x0D);
			if(ask_bt(&at_commands[0])==0){
				sprintf(at_commands,"ATN=SMARTSPEEDO%c",0x0D);
				if(ask_bt(&at_commands[0])==0){
#if TARGET_UART_SPEED == 115200
					sprintf(at_commands,"ATL5%c",0x0D);
					ask_bt(&at_commands[0]);											// fire && forget
#endif
//					pOLED->string_P(pSpeedo->default_font,PSTR("OK"),14,5);				// wird schon auf anderer geschwindigkeit geantwortet, können wir hier nicht testen
					Serial.end();														// setzte neue serielle Geschwindigkeit
//					pOLED->string_P(pSpeedo->default_font,PSTR("RETRYING"),0,6);		// hat nicht geklappt
					Serial.begin(TARGET_UART_SPEED);
					_delay_ms(2000);
//					pOLED->clear_screen();
//					pOLED->string_P(pSpeedo->default_font,PSTR("Checking connection"),0,0);
					sprintf(at_commands,"AT%c",0x0D);									// gleich neu testen
					if(ask_bt(&at_commands[0])==0){
						connection_established=true;
//						pOLED->string_P(pSpeedo->default_font,PSTR("OK"),14,1);
					};
				}
			}
		} else {
//			pOLED->string_P(pSpeedo->default_font,PSTR("FAILED"),14,3);
//			pOLED->string_P(pSpeedo->default_font,PSTR("DAMN"),14,4); // add message to make sure no active bt connection disturbs TODO
			_delay_ms(4000);
			Serial.end();
			Serial.begin(115200);
			return -9;
		}
	}
	Serial.println("Verbindung hergestellt"); //TODO

	if(connection_established){
//		pOLED->filled_rect(0,8,128,56,0x00); // clear the lower lines
//		pOLED->string_P(pSpeedo->default_font,PSTR("OK"),14,1);
//		pOLED->string_P(pSpeedo->default_font,PSTR("Activating responses"),0,2);
		sprintf(at_commands,"ATQ0%c",0x0D); // schaltet result codes ein				// jetzt richtig
		if(ask_bt(&at_commands[0])==0){
//			pOLED->string_P(pSpeedo->default_font,PSTR("OK"),14,3);
//			pOLED->string_P(pSpeedo->default_font,PSTR("Setting PIN Code"),0,4);
			int bt_pin=0000;
			sprintf(at_commands,"ATP=%04i%c",bt_pin,13);

			if(ask_bt(&at_commands[0])==0){
//				pOLED->string_P(pSpeedo->default_font,PSTR("OK"),14,5);
//				pOLED->string_P(pSpeedo->default_font,PSTR("Deactivating response"),0,6);
				sprintf(at_commands,"ATQ0%c",13); // schaltet result codes ein

				if(ask_bt(&at_commands[0])==0){
//					pOLED->string_P(pSpeedo->default_font,PSTR("OK"),14,7);
					_delay_ms(2000);
					return 0;
				}
			}
		}
	} else {
		ask_bt(&at_commands[0]);
//		pOLED->string_P(pSpeedo->default_font,PSTR("failed, hmmm"),0,1);
		_delay_ms(5000);
	}
	_delay_ms(2000);
	return -2;
}

bool Speedo_aktors::check_mac_key(uint8_t* result,bool* comm_error){
	char at_commands[22];
	sprintf_P(at_commands,PSTR("ATB?%c"),0x0D);
	uint8_t returns=0;
	if(ask_bt(at_commands,true,22,&returns)==0){
		if(returns>=17){
			*comm_error=false;
			// kick sondernzeichern
			int char_array_pointer=0;
			for(uint8_t n=0;n<returns; n++){
				if((at_commands[n]>='0' && at_commands[n]<='9') || (at_commands[n]>='a' && at_commands[n]<='f')){
					at_commands[char_array_pointer]=at_commands[n];
					char_array_pointer++;
				} else if(at_commands[n]==' ' || char_array_pointer>=12){
					at_commands[char_array_pointer]=0x00;
					n=returns;
					returns=char_array_pointer;
				}
			}

			// move it from ascii hex to hex
			for(uint8_t n=0;n<char_array_pointer; n++){
				if(at_commands[n]>='0' && at_commands[n]<='9'){
					at_commands[n]-='0';
				}
				if(at_commands[n]>='a' && at_commands[n]<='f'){
					at_commands[n]-='a'-10;
				}
				if(at_commands[n]>='A' && at_commands[n]<='F'){
					at_commands[n]-='A'-10;
				}
			}

			// calc checksum
			uint16_t temp=0x00;
			for(uint8_t n=0;n<6;n++){
				temp+=((at_commands[n*2]&0x0f)<<4)+(at_commands[n*2+1]&0x0f);
			}
			temp=temp&0xff; // cut it to 8-bit

			if(		temp==0xD2 || // Kolja			= 00+12+6F+28+3C+ED
					temp==0xC8 || // Thomas			= 00+12+6F+28+3C+E3
					temp==0xC7 || // Phil			= 00+12+6F+28+3C+E2
					temp==0xD3 || // Patrick		= 00+12+6F+28+3C+EE
					temp==0xA3 || // Devel-Speedo 	= 00+12+6f+22+45+bb
					temp==0xC6){  // Andrej			= 00+12+6F+28+3C+E1
				*result=temp;
				return true;
			}
		} else { // if no answere -> break
			*result=0x00;
			*comm_error=true;
			return true;
		}
	}
	return false;
}


int Speedo_aktors::ask_bt(char *command){
	uint8_t temp;
	return ask_bt(command,false,0,&temp); // by default no answere
}

int Speedo_aktors::ask_bt(char *buffer, bool answere_needed, int8_t max_length, uint8_t* char_rec){
	for(int looper=0;looper<30;looper++){
		Serial.print(buffer);
		// A T \r \n O K \r \n = 8
		//warte bis der Buffer nicht voller wird
		_delay_ms(200);

		uint8_t ok_state=0; // 0 teile von "o" "k"
		int8_t n=0;
		*char_rec=0;
		char answere[20];
		while(Serial.available()){
			char temp = Serial.read();
			if(answere_needed){
				*char_rec=*char_rec+1;
				if(n<(max_length-1)){
					answere[n]=temp;
					n++;
					answere[n]=0x00; // EOString, 0x00 will be MAX in (max-length-1)
				};
			};

			if(temp=='O'){
				ok_state=1; // 1. TEIL
			} else if(temp=='K' && ok_state==1){
				strcpy(buffer,answere);
				return 0;

			} else {
				ok_state=0;
			}
		};
	};
	return -1;
}



