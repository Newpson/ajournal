#version 100

/* #define SURFACE_WIDTH ${SURFACE_WIDTH} */
/* #define SURFACE_HEIGHT ${SURFACE_HEIGHT} */
/* #define SURFACE_RATIO SURFACE_WIDTH/SURFACE_HEIGHT */
#define WACOM_WIDTH ${WACOM_WIDTH}
#define WACOM_HEIGHT ${WACOM_HEIGHT}
#define WACOM_SENSITIVITY ${WACOM_SENSITIVITY}

/* FIXME in px of surface */
#define MAX_STROKE_RADIUS 0.0075

/* rotate by pi/2 */
const mat2 R = mat2(0.0, 1.0, -1.0, 0.0);

attribute vec4 in_prev;
attribute vec4 in_cur;
attribute vec4 in_next;

float x2unit(in float x)
{
	return (2.0*x / WACOM_WIDTH) - 1.0;
}

float y2unit(in float y)
{
	return 1.0 - (2.0*y / WACOM_HEIGHT);
}

float z2unit(in float z)
{
	return z/WACOM_SENSITIVITY * MAX_STROKE_RADIUS;
}

vec2 v2unit(in vec2 v)
{
	return vec2(x2unit(v.x), y2unit(v.y));
}

void main()
{
	vec2 prev = v2unit(in_prev.xy);
	vec2 cur = v2unit(in_cur.xy);
	vec2 next = v2unit(in_next.xy);

	float dir = in_prev.w * in_cur.w * in_next.w;

	vec2 normal = R*normalize(normalize(next-cur) + normalize(cur-prev));
	gl_Position = vec4(cur + z2unit(in_cur.z)*dir*normal, 0.0, 1.0);
}

