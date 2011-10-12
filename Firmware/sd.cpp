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
speedo_sd::speedo_sd(){
};

speedo_sd::~speedo_sd(){
};

// store error strings in flash to save RAM
void speedo_sd::error_P(const char* str) {
	PgmPrint("error: ");
	SerialPrintln_P(str);
	if (card.errorCode()) {
		PgmPrint("SD error: ");
		Serial.print(card.errorCode(), HEX);
		PgmPrint(",");
		Serial.println(card.errorData(), HEX);
	}
}
/*
 * Write CR LF to a file
 */
void speedo_sd::writeCRLF(SdFile& f) {
	f.write((uint8_t*)"\r\n", 2);
}
/*
 * Write a string to a file
 */
int speedo_sd::writeString(SdFile& f, char *str) {
	uint8_t n;
	for (n = 0; str[n]; n++); // n=zähler, hier sind keine klammern
	return f.write((uint8_t *)str, n);
}

void speedo_sd::init(){
	sd_failed=false;
	bool allright=true;
	int tries=0;
	while(tries<3){ //maximal 3 versuche die sd karte zu öffnen
		if (!card.init(SPI_HALF_SPEED))	{	error("card.init failed"); 		allright=false; };
		if (!volume.init(&card))		{ 	error("volume.init failed"); 	allright=false; }; // initialize a FAT volume
		if(allright){
			break;
		} else {
			tries++;
			allright=true;
		};
	};
	if(!allright){
		sd_failed=true;
		Serial.println("hier 1");
	};
	Serial.println("SD init done");
};