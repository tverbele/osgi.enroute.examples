#include <jni.h>
#include "osgi_enroute_rube_goldberg_camera_V4L2Camera.h"
#include "camera.h"
#include <stdio.h>

#define MAX_DEVICES 5

camera_t* cameras[MAX_DEVICES]; // hard code max number of cameras?

JNIEXPORT jint JNICALL Java_osgi_enroute_rube_goldberg_camera_V4L2Camera_open_1device
  (JNIEnv * env, jobject obj, jstring deviceString){
	// convert string to native char*
	const char* device = (*env)->GetStringUTFChars(env, deviceString, 0);

	// initialize device
	int index = -1;
	int i;
	for(i=0;i<MAX_DEVICES;i++){
		if(cameras[i]==0){
			index = i;
			break;
		}
	}
	if(index==-1){
		// no room for other devcie
		fprintf(stderr, "No more than %d simultaneous devices allowed \n", MAX_DEVICES);
		return -1;
	}

	camera_t* cam = (camera_t*)malloc(sizeof(camera_t));
	if(!open_device(cam, device)){
		fprintf(stderr, "Failed to open device %s\n", device);
		return -1;
	}
	cameras[index] = cam;

	// release string
	(*env)->ReleaseStringUTFChars(env, deviceString, device);

	return index;
}


JNIEXPORT jobject JNICALL Java_osgi_enroute_rube_goldberg_camera_V4L2Camera_start_1capturing
  (JNIEnv * env, jobject obj, jint index, jint width, jint height, jint format){
	camera_t* cam = cameras[index];

	// initialize device width/height
	init_device(cam, width, height, format);

	// memory map v4l2 buffers
	init_mmap(cam);

	// create frame buffer (accessible from Java)
	cam->format = format;
	int framesize;
	if(format==0){ // GRAYSCALE
		framesize = cam->width*cam->height;
	} else if(format==1){ // RGB
		framesize = cam->width*cam->height*3;
	} else if(format==2){ //MJPG
		framesize = cam->width*cam->height*2;
	}
	cam->frame = malloc(framesize);

	// start streaming
	start_capturing(cam);

	// return bytebuffer
	jobject bytebuffer = (*env)->NewDirectByteBuffer(env, cam->frame, framesize);
	return bytebuffer;
}

JNIEXPORT jint JNICALL Java_osgi_enroute_rube_goldberg_camera_V4L2Camera_next_1frame
  (JNIEnv * env, jobject obj, jint index){
	camera_t* cam = cameras[index];

	return frame_get(cam);
}


JNIEXPORT void JNICALL Java_osgi_enroute_rube_goldberg_camera_V4L2Camera_stop_1capturing
  (JNIEnv * env, jobject obj, jint index){
	camera_t* cam = cameras[index];

	stop_capturing(cam);
	uninit_mmap(cam);

	free(cam->frame);
}

JNIEXPORT void JNICALL Java_osgi_enroute_rube_goldberg_camera_V4L2Camera_close_1device
  (JNIEnv * env, jobject obj, jint index){
	camera_t* cam = cameras[index];

	// close device;
	close_device(cam);

	free(cam);
	cameras[index] = 0;
}

JNIEXPORT jint JNICALL Java_osgi_enroute_rube_goldberg_camera_V4L2Camera_get_1width
  (JNIEnv * env, jobject obj, jint index){
	camera_t* cam = cameras[index];
	return cam->width;
}


JNIEXPORT jint JNICALL Java_osgi_enroute_rube_goldberg_camera_V4L2Camera_get_1height
  (JNIEnv * env, jobject obj, jint index){
	camera_t* cam = cameras[index];
	return cam->height;
}
