/*
 * menu.h
 *
 *  Created on: 01.06.2011
 *      Author: kolja
 */

#ifndef MENU_H_
#define MENU_H_
////////////// menu ///////////////////////
class speedo_menu {

#define		menu_button_timeout			300                        // wartezeit zwischen 2 steps
#define     menu_button_fast_delay		menu_button_timeout*4   // wartezeit bis schneller
#define     menu_button_fast_timeout	100                   // wartezeit zwischen 2 steps -> schnell

public:
	speedo_menu(void);
	~speedo_menu();
	void init();
	void display();
	bool button_test(bool bt_keys_en);
	void back();
	void draw(const char** menu, int entries);
	void yesno(char first[30],char second[30],char third[30]);
	void del_conf(char first[20],char second[20]);

	unsigned long state;
	bool button_rechts_valid;
	bool button_links_valid;
	bool button_oben_valid;
	bool button_unten_valid;

private:
	unsigned long button_time;                      // Zeitpunkt des letzen Button kontakt
	unsigned long button_first_push;                // für "halten". Zeitpunkt des ersten Kontakt

};
extern speedo_menu* pMenu;
////////////// menu ///////////////////////
#endif /* MENU_H_ */