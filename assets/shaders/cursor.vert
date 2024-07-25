#version 100

attribute vec2 in_pos;

uniform mat4 projectionM;

void main()
{
	gl_Position = projectionM * vec4(in_pos, 0.0, 1.0);
}

