
#ifndef CAMERA_H
#define CAMERA_H

#include <linux/videodev2.h>

typedef struct buffer {
	size_t length;
	char *start;
} buffer_t;

typedef struct camera {
	int dev; // camera device number 0..N
	int fd; // camera file descriptor for video_dev

	buffer_t * buffers; // buffers for mmap
	unsigned int n_buffers; // number of buffers

	int width; // width of the camera
	int height;  // height of the camera
	int format; // pixel format?

	char* frame; // current frame data

	struct v4l2_capability cap; // v4l2 caps
} camera_t;


#endif
