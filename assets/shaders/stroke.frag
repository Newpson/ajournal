#version 100
precision mediump float;

uniform vec4 colors[10];
varying float v_colori;

void main()
{
	gl_FragColor = colors[int(v_colori)-1];
}
