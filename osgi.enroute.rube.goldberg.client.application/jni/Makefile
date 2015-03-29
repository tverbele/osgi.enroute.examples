CC := gcc
JAVAH := osgi.enroute.rube.goldberg.camera.V4L2Camera
LIB := libCamera.so
OBJECT := capture.o camera.o 
SRC := capture.c camera.c 
INCLUDES := -I/usr/lib/jvm/jdk1.7.0/include/ -I/usr/lib/jvm/jdk1.7.0/include/linux
LINKS:= 
CFLAGS:= -Wall -march=core2 -msse3 -O3 


all: install

javah:
	javah -jni -classpath ../bin:../../osgi.enroute.rube.goldberg.api/bin $(JAVAH)  
	
compile: javah
	$(CC) $(CFLAGS) -fPIC -DLINUX $(INCLUDES) -c $(SRC)

link: compile 
	$(CC) $(CFLAGS) -shared $(OBJECT) $(LINKS) -o $(LIB) 
	
install: link
	cp $(LIB) ../native/x86_64/$(LIB)


clean: 
	rm be_iminds_mercator_source_camera_Camera.h
	rm $(OBJECT)
	rm $(LIB)
	
	 