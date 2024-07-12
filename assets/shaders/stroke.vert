#version 100

/* rotate by pi/2 */
const mat2 R = mat2(0.0, 1.0, -1.0, 0.0);

attribute vec4 in_prev;
attribute vec4 in_cur;
attribute vec4 in_next;

void main()
{
	vec2 prev = in_prev.xy;
	vec2 cur = in_cur.xy;
	vec2 next = in_next.xy;

	float dir = in_prev.w * in_cur.w * in_next.w;

	vec2 normal = R*normalize(normalize(next-cur) + normalize(cur-prev));
	gl_Position = vec4(cur + in_cur.z*dir*normal, 0.0, 1.0);
}

