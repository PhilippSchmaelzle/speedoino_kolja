################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../aktors.cpp \
../blinker.cpp \
../clock_me.cpp \
../config.cpp \
../debug.cpp \
../display.cpp \
../dz.cpp \
../file_manager_v2.cpp \
../fuel.cpp \
../gear.cpp \
../gps.cpp \
../helper.cpp \
../main.cpp \
../menu.cpp \
../oiler.cpp \
../reset.cpp \
../sd.cpp \
../sensors.cpp \
../speed.cpp \
../speedo.cpp \
../sprint.cpp \
../stepper.cpp \
../temperature.cpp \
../tetris.cpp \
../timer.cpp \
../voltage.cpp 

OBJS += \
./aktors.o \
./blinker.o \
./clock_me.o \
./config.o \
./debug.o \
./display.o \
./dz.o \
./file_manager_v2.o \
./fuel.o \
./gear.o \
./gps.o \
./helper.o \
./main.o \
./menu.o \
./oiler.o \
./reset.o \
./sd.o \
./sensors.o \
./speed.o \
./speedo.o \
./sprint.o \
./stepper.o \
./temperature.o \
./tetris.o \
./timer.o \
./voltage.o 

CPP_DEPS += \
./aktors.d \
./blinker.d \
./clock_me.d \
./config.d \
./debug.d \
./display.d \
./dz.d \
./file_manager_v2.d \
./fuel.d \
./gear.d \
./gps.d \
./helper.d \
./main.d \
./menu.d \
./oiler.d \
./reset.d \
./sd.d \
./sensors.d \
./speed.d \
./speedo.d \
./sprint.d \
./stepper.d \
./temperature.d \
./tetris.d \
./timer.d \
./voltage.d 


# Each subdirectory must supply rules for building sources it contributes
%.o: ../%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: AVR C++ Compiler'
	avr-g++ -I"/home/jkw/Store/17 - Speedmaster/ATm2560_Firmware" -I"/home/jkw/Store/17 - Speedmaster/ATm2560_Firmware/inc" -I/usr/lib/avr/include/ -Wall -Os -fpack-struct -fshort-enums -funsigned-char -funsigned-bitfields -fno-exceptions -v -lm -mmcu=atmega2560 -DF_CPU=16000000UL -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -c -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '

