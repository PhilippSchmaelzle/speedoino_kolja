/*
 * display.h
 *
 *  Created on: 01.06.2011
 *      Author: kolja
 */

#ifndef DISPLAY_H_
#define DISPLAY_H_

////////////// DISPLAY ///////////////////////////////////
#include      <ssd0323.h>
class speedo_disp : public ssd0323 {
#define DISP_BRIGHTNESS 15
#define DIALOG_NO_YES 1
public:
	unsigned char 	phase;//=0x62;
	unsigned char 	ref;//=0x3F;
	bool       		disp_invert;
	unsigned long 	disp_last_invert;
	unsigned char startup[200];
	void init_speedo();
	speedo_disp(void);
	~speedo_disp();
	void draw_oil(unsigned char x,unsigned char y);
	void draw_water(unsigned char x,unsigned char y);
	void draw_air(unsigned char x,unsigned char y);
	void draw_fuel(unsigned char x,unsigned char y);
	void draw_clock(unsigned char x,unsigned char y);
	void draw_gps(unsigned char x,unsigned char y, unsigned char sats);
	void draw_blitzer(unsigned char x,unsigned char y);
	void draw_arrow(int arrow, int spalte, int zeile);
	int  sd2ssd(char filename[10],int frame);
	void show_animation(unsigned char *command);
	void disp_waiting(int position,unsigned char spalte,unsigned char zeile);
	int animation(int a);
	void show_storry(char storry[],unsigned int storry_length,char title[],unsigned int title_length);
	void show_storry(char storry[],unsigned int storry_length,char title[],unsigned int title_length, uint8_t type);
	void show_storry(const char* storry,const char* title);
	void show_storry(const char* storry,const char* title, uint8_t type);
};
extern speedo_disp* pOLED;
////////////// DISPLAY ///////////////////////////////////

#endif /* DISPLAY_H_ */
