#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <getopt.h>             /* getopt_long() */

#include <fcntl.h>              /* low-level i/o */
#include <unistd.h>
#include <errno.h>
#include <malloc.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/mman.h>
#include <sys/ioctl.h>

#include <asm/types.h>          /* for videodev2.h */

#include <linux/videodev2.h>

#include "camera.h"

#define NUM_BUFFERS         3

#define CLEAR(x)            memset(&(x), 0, sizeof(x))

static int xioctl(int fd, int request, void* arg) {
	int r;
	do
		r = ioctl(fd, request, arg);
	while (-1 == r && EINTR == errno);
	return r;
}


int open_device(camera_t* cam, char* dev_name) {

	struct stat st;
	int fd = -1;

	if (-1 == stat(dev_name, &st)) {
		fprintf(stderr, "Cannot identify '%s': %d, %s\n", dev_name, errno,
				strerror(errno));
		return 0;
	}

	if (!S_ISCHR(st.st_mode)) {
		fprintf(stderr, "%s is no device\n", dev_name);
		return 0;
	}

	fd = open(dev_name, O_RDWR /* required */| O_NONBLOCK, 0);

	if (-1 == fd) {
		fprintf(stderr, "Cannot open '%s': %d, %s\n", dev_name, errno,
				strerror(errno));
		return 0;
	}

	cam->fd = fd;

	if(xioctl(cam->fd, VIDIOC_QUERYCAP, &cam->cap) == -1) {
		fprintf(stderr, "Could not connect to video device (%s): %d, %s\n",
				dev_name, errno, strerror(errno));
		return 0;
	}

	if (!(cam->cap.capabilities & V4L2_CAP_VIDEO_CAPTURE)) {
		fprintf(stderr, "Device is no video capture device\n");
		return 0;
	}

	if (!(cam->cap.capabilities & V4L2_CAP_STREAMING)) {
		fprintf(stderr, "Device does not support streaming i/o\n");
		return 0;
	}

	return 1;
}


void init_device(camera_t* cam, int w, int h, int f) {
	struct v4l2_cropcap cropcap;
	struct v4l2_crop crop;
	struct v4l2_format fmt;
	unsigned int min;

	/* Select video input, video standard and tune here. */
	CLEAR (cropcap);

	cropcap.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;

	if (0 == xioctl(cam->fd, VIDIOC_CROPCAP, &cropcap)) {
		crop.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
		crop.c = cropcap.defrect; /* reset to default */

		xioctl(cam->fd, VIDIOC_S_CROP, &crop);
	}

	CLEAR (fmt);

	fmt.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
	fmt.fmt.pix.width = w;
	fmt.fmt.pix.height = h;
	if(f==2){
		fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_MJPEG;
	} else {
		fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_YUYV; // use YUYV format (mostly supported)
	}

	xioctl(cam->fd, VIDIOC_S_FMT, &fmt);

	/* Note VIDIOC_S_FMT may change width and height. */
	cam->width = fmt.fmt.pix.width;
	cam->height = fmt.fmt.pix.height;

	/* Buggy driver paranoia. */
	min = fmt.fmt.pix.width * 2;
	if (fmt.fmt.pix.bytesperline < min)
		fmt.fmt.pix.bytesperline = min;
	min = fmt.fmt.pix.bytesperline * fmt.fmt.pix.height;
	if (fmt.fmt.pix.sizeimage < min)
		fmt.fmt.pix.sizeimage = min;

}


void init_mmap(camera_t* cam) {
	struct v4l2_requestbuffers req;

	CLEAR (req);

	req.count = NUM_BUFFERS;
	req.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
	req.memory = V4L2_MEMORY_MMAP;

	xioctl(cam->fd, VIDIOC_REQBUFS, &req);

	if (req.count < 2) {
		fprintf(stderr, "Insufficient buffer memory on device\n");
	}

	cam->n_buffers = req.count;
	cam->buffers = calloc(req.count, sizeof(*cam->buffers));

	if (!cam->buffers) {
		fprintf(stderr, "Out of memory\n");
	}

	int n_buffers;
	for (n_buffers = 0; n_buffers < req.count; ++n_buffers) {
		struct v4l2_buffer buf;

		CLEAR (buf);

		buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
		buf.memory = V4L2_MEMORY_MMAP;
		buf.index = n_buffers;

		xioctl(cam->fd, VIDIOC_QUERYBUF, &buf);

		cam->buffers[n_buffers].length = buf.length;
		cam->buffers[n_buffers].start = mmap(NULL /* start anywhere */, buf.length,
				PROT_READ | PROT_WRITE /* required */,
				MAP_SHARED /* recommended */, cam->fd, buf.m.offset);
	}
}

void start_capturing(camera_t* cam) {
	unsigned int i;
	enum v4l2_buf_type type;

	for (i = 0; i < cam->n_buffers; ++i) {
		struct v4l2_buffer buf;

		CLEAR (buf);

		buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
		buf.memory = V4L2_MEMORY_MMAP;
		buf.index = i;

		xioctl(cam->fd, VIDIOC_QBUF, &buf);
	}

	type = V4L2_BUF_TYPE_VIDEO_CAPTURE;

	xioctl(cam->fd, VIDIOC_STREAMON, &type);
}

void stop_capturing(camera_t* cam) {
	enum v4l2_buf_type type;

	type = V4L2_BUF_TYPE_VIDEO_CAPTURE;

	xioctl(cam->fd, VIDIOC_STREAMOFF, &type);

}

void uninit_mmap(camera_t* cam) {
	unsigned int i;
	for (i = 0; i < cam->n_buffers; ++i)
		munmap(cam->buffers[i].start, cam->buffers[i].length);

	free(cam->buffers);
}


void close_device(camera_t* cam) {
	close(cam->fd);
}


/*
 * Convertion helper functions
 */

static void yuv422_to_rgb(const unsigned char* yuv, const unsigned char* rgb, unsigned int width, unsigned int height)
{
	int yy, uu, vv, ug_plus_vg, ub, vr;
	int r,g,b;

	// separate band for r g b
	unsigned char* r_ptr;
	unsigned char* g_ptr;
	unsigned char* b_ptr;
	r_ptr = rgb;
	g_ptr = rgb+1;
	b_ptr = rgb+2;

	unsigned int total = width*height;
	total /= 2;
	while (total--) {
	    	yy = yuv[0] << 8;
	    	uu = yuv[1] - 128;
	    	vv = yuv[3] - 128;
	    	ug_plus_vg = uu * 88 + vv * 183;
	    	ub = uu * 454;
	    	vr = vv * 359;
	    	r = (yy + vr) >> 8;
	    	g = (yy - ug_plus_vg) >> 8;
	    	b = (yy + ub) >> 8;
	    	*(r_ptr) = r < 0 ? 0 : (r > 255 ? 255 : (unsigned char)r);
	    	*(g_ptr) = g < 0 ? 0 : (g > 255 ? 255 : (unsigned char)g);
	    	*(b_ptr) = b < 0 ? 0 : (b > 255 ? 255 : (unsigned char)b);
	    	r_ptr+=3;
	    	g_ptr+=3;
	    	b_ptr+=3;

	    	yy = yuv[2] << 8;
	    	r = (yy + vr) >> 8;
	    	g = (yy - ug_plus_vg) >> 8;
	    	b = (yy + ub) >> 8;
	    	*(r_ptr) = r < 0 ? 0 : (r > 255 ? 255 : (unsigned char)r);
	    	*(g_ptr) = g < 0 ? 0 : (g > 255 ? 255 : (unsigned char)g);
	    	*(b_ptr) = b < 0 ? 0 : (b > 255 ? 255 : (unsigned char)b);
	    	r_ptr+=3;
	    	g_ptr+=3;
	    	b_ptr+=3;

	    	yuv += 4;
	}
}

static void yuv422_to_grey(const unsigned char* yuv, unsigned char* grey, unsigned int width, unsigned int height)
{
	unsigned int total = width*height;
	total/=2;
	while (total--) {
		grey[0] = yuv[0];
	    grey[1] = yuv[2];
	    yuv += 4;
	    grey += 2;
	}
}

int frame_get(camera_t* cam) {
	fd_set fds;
	struct timeval tv;
	int r;

	FD_ZERO(&fds);
	FD_SET(cam->fd, &fds);

	/* Timeout. */
	tv.tv_sec = 1;
	tv.tv_usec = 0;

	r = select(cam->fd + 1, &fds, NULL, NULL, &tv);

	if (-1 == r) {
		if (EINTR == errno){
			fprintf(stderr, "select error\n");
			return 0;
		}
	}

	if (0 == r) {
		fprintf(stderr, "select timeout\n");
		return 0;
	}

	// read frame
	struct v4l2_buffer buf;

	CLEAR (buf);

	buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
	buf.memory = V4L2_MEMORY_MMAP;

	if (-1 == xioctl(cam->fd, VIDIOC_DQBUF, &buf)) {
		switch (errno) {
		case EAGAIN:
			return 0;

		case EIO:
			/* Could ignore EIO, see spec. */

			/* fall through */

		default:
			fprintf(stderr, "VIDIOC_DQBUF error \n");
			return -1;
		}
	}

	assert(buf.index < cam->n_buffers);

	int size = (int)cam->buffers[buf.index].length;

	if(cam->format==0){
		yuv422_to_grey(cam->buffers[buf.index].start, cam->frame, cam->width, cam->height);
	} else if(cam->format==1){
		yuv422_to_rgb(cam->buffers[buf.index].start, cam->frame, cam->width, cam->height);
	} else if(cam->format==2){
		memcpy(cam->frame, cam->buffers[buf.index].start, cam->buffers[buf.index].length);
	}

	if (-1 == xioctl(cam->fd, VIDIOC_QBUF, &buf)){
		fprintf(stderr, "VIDIOC_QBUF error \n");
	}

	return size;
}

