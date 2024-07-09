#version 100

#define WACOM_WIDTH ${WACOM_WIDTH}
#define WACOM_HEIGHT ${WACOM_HEIGHT}

attribute vec2 in_pos;

float x2unit(in float x)
{
	return (2.0*x / WACOM_WIDTH) - 1.0;
}

float y2unit(in float y)
{
	return 1.0 - (2.0*y / WACOM_HEIGHT);
}

vec2 v2unit(in vec2 v)
{
	return vec2(x2unit(v.x), y2unit(v.y));
}

void main()
{
	gl_Position = vec4(v2unit(in_pos), 0.0, 1.0);
}

