package newpson.ajournal;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.AttributeSet;
import android.util.Log;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import static android.opengl.GLES20.*;
import static javax.microedition.khronos.egl.EGL10.*;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

public class AJSurface extends GLSurfaceView implements GLSurfaceView.Renderer, GLSurfaceView.EGLConfigChooser
{
	public static final int MAX_MEMORY_RATE = 1572864; /* 1.5 MB is OK */
	public static final float COLOR_BLUE = 1.0f;
	public static final float COLOR_ORANGE = 2.0f;
	public static final float COLOR_RED = 3.0f;
	public static final float COLOR_CYAN = 4.0f;
	public static final float COLOR_GREEN = 5.0f;
	public static final float COLOR_YELLOW = 6.0f;
	public static final float COLOR_PURPLE = 7.0f;
	public static final float COLOR_PINK = 8.0f;
	public static final float COLOR_BROWN = 9.0f;
	public static final float COLOR_GRAY = 10.0f;
	private boolean showCursor = true;
	private boolean straight = false;
	private float color = COLOR_BLUE;
	private float thickness = 10.0f;
	private float pressureFactor = 0.0f;

	/**
	 * sv_ - vertex shader
	 * sf_ - fragment shader
	 * p_ - program
	 * b_ - buffer 
	 * u_ - uniform 
	 * a_ - attribute
	 */
	private int p_stroke;
	private int sv_stroke;
	private int sf_stroke;

	private int p_cursor;
	private int sv_cursor;
	private int sf_cursor;

	private int b_array;

	private int u_stroke_projectionM;
	private int u_stroke_colors;
	private int u_cursor_projectionM;

	private int a_pos = 0; /* manual indexing is important !!! 4 hours, my dudes ... */
	private int a_prev = 1;
	private int a_cur = 2;
	private int a_next = 3;

	private int bufi = 0;
	private int vertc = 0;
	float[] update = new float[4*2];
	float[] cursq = new float[2*4]; /* cursor square */
	float[] colors = new float[]
	{ /* Tableau 10 palette */
		0.306f, 0.475f, 0.655f, 1.0f, /* blue */
		0.949f, 0.557f, 0.169f, 1.0f, /* orange */
		0.882f, 0.341f, 0.349f, 1.0f, /* red */
		0.463f, 0.718f, 0.698f, 1.0f, /* cyan */
		0.349f, 0.631f, 0.310f, 1.0f, /* green */
		0.929f, 0.788f, 0.282f, 1.0f, /* yellow */
		0.690f, 0.478f, 0.631f, 1.0f, /* purple */
		1.000f, 0.616f, 0.655f, 1.0f, /* pink */
		0.612f, 0.459f, 0.373f, 1.0f, /* brown */
		0.729f, 0.690f, 0.675f, 1.0f, /* gray */
	};
	private float[] projectionM = new float[16];
	private Random random = new Random();

	public AJSurface(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		setEGLConfigChooser(this);
		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	private float radius(float pressure)
	{
		return (pressureFactor*pressure + (1.0f-pressureFactor)) * thickness;
	}

	public void dragStart(float x, float y, float p)
	{
		queueEvent(new Runnable()
		{
			@Override
			public void run()
			{
				if (straight)
				{
					addPoint(x, y, 0.0f); 
					addPoint(x, y, thickness);
					vertc += 8;
				}
				else
				{
					addPoint(x, y, 0.0f); /* prev */
					addPoint(x, y, radius(p)); /* cur */
					vertc += 4;
				}
			}
		});
	}

	public void move(float x, float y)
	{
		queueEvent(new Runnable()
		{
			@Override
			public void run()
			{
				setCursor(x, y);
			}
		});
		requestRender();
	}

	public void drag(float x, float y, float p)
	{
		queueEvent(new Runnable()
		{
			@Override
			public void run()
			{
				if (straight)
				{
					addPoint(x, y, thickness);
					addPoint(x, y, thickness);
					bufi -= 16;
				}
				else
				{
					addPoint(x, y, radius(p)); /* cur */
					addPoint(x, y, radius(p)); /* next */
					bufi -= 8; /* modify cur */
					vertc += 2;
				}
			}
		});
		requestRender();
	}

	public void dragStop(float x, float y, float p)
	{
		queueEvent(new Runnable()
		{
			@Override
			public void run()
			{
				if (straight)
				{
					addPoint(x, y, thickness);
					addPoint(x, y, 0.0f);
				}
				else
				{
					addPoint(x, y, radius(p));
					addPoint(x, y, 0.0f);
					vertc += 4;
				}
			}
		});
		requestRender();
	}

	public void onSurfaceCreated(GL10 unused, EGLConfig config)
	{
		/* Shader calls must be in one of these methods that are running in OpenGL threads!!! */
		/* I spent about 4 hours solving this problem... */
		/* TODO if p_stroke was not created */

		/* .-= COMMON =-. */
		int[] buffers = new int[1];
		glGenBuffers(1, buffers, 0);
		b_array = buffers[0];
		glBindBuffer(GL_ARRAY_BUFFER, b_array);
		glBufferData(GL_ARRAY_BUFFER, Float.BYTES*8 + MAX_MEMORY_RATE, null, GL_DYNAMIC_DRAW); /* + cursor square at very beginning */

		/* .-= SHADERS =-. */
		try
		{
			sv_stroke = raiseShader("shaders/stroke.vert");
			sf_stroke = raiseShader("shaders/stroke.frag");
			sv_cursor = raiseShader("shaders/cursor.vert");
			sf_cursor = raiseShader("shaders/cursor.frag");
		} catch (Exception exc)
		{
			Log.e("ajournal-debug", exc.getMessage());
			/* TODO ... kill process please... or stop running further */
		}

		/* .-= STROKE PROGRAM =-. */
		p_stroke = glCreateProgram();
		glAttachShader(p_stroke, sv_stroke);
		glAttachShader(p_stroke, sf_stroke);

		glBindAttribLocation(p_stroke, a_prev, "in_prev");
		glBindAttribLocation(p_stroke, a_cur, "in_cur");
		glBindAttribLocation(p_stroke, a_next, "in_next");
		glVertexAttribPointer(a_prev, 4, GL_FLOAT, false, Float.BYTES*4, 0);
		glVertexAttribPointer(a_cur, 4, GL_FLOAT, false, Float.BYTES*4, 32);
		glVertexAttribPointer(a_next, 4, GL_FLOAT, false, Float.BYTES*4, 64);
		glEnableVertexAttribArray(a_prev);
		glEnableVertexAttribArray(a_cur);
		glEnableVertexAttribArray(a_next);
		glLinkProgram(p_stroke);

		u_stroke_projectionM = glGetUniformLocation(p_stroke, "projectionM"); /* get location only after linking! */
		u_stroke_colors = glGetUniformLocation(p_stroke, "colors");

		glUseProgram(p_stroke);
		glUniform4fv(u_stroke_colors, 10, colors, 0);

		/* .-= CURSOR PROGRAM =-. */
		p_cursor = glCreateProgram();
		glAttachShader(p_cursor, sv_cursor);
		glAttachShader(p_cursor, sf_cursor);

		glBindAttribLocation(p_cursor, a_pos, "in_pos");
		glVertexAttribPointer(a_pos, 2, GL_FLOAT, false, Float.BYTES*2, 0);
		glEnableVertexAttribArray(a_pos);
		glLinkProgram(p_cursor);

		u_cursor_projectionM = glGetUniformLocation(p_cursor, "projectionM");

		glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	public void onDrawFrame(GL10 unused)
	{
		glClear(GL_COLOR_BUFFER_BIT);

		glUseProgram(p_stroke);
		glDrawArrays(GL_TRIANGLE_STRIP, 2 /* skip 8*Float.BYTES bytes */, vertc-4);

		if (showCursor)
		{
			glUseProgram(p_cursor);
			glDrawArrays(GL_LINES, 0, 4);
		}
	}

	public void hideCursor(boolean hide)
	{
		showCursor = !hide;
		requestRender();
	}

	public void setColor(float color)
	{
		this.color = color;
	}

	public void setPressureFactor(float factor)
	{
		pressureFactor = factor;
	}

	public void setThickness(float thickness)
	{
		this.thickness = thickness;
	}

	public void setStraight(boolean straight)
	{
		this.straight = straight;
	}

	public void clear()
	{
		vertc = 0;
		bufi = 0;
		requestRender();
	}

	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		glViewport(0, 0, width, height);
		Matrix.orthoM(projectionM, 0, 0.0f, (float) width, (float) height, 0.0f, -1.0f, 1.0f);
		glUseProgram(p_stroke); /* 6 hours spent for this line (ALWAYS LOAD PROGRAMS BEFORE USING THEIR VARS) */
		glUniformMatrix4fv(u_stroke_projectionM, 1, false, projectionM, 0);
		glUseProgram(p_cursor);
		glUniformMatrix4fv(u_cursor_projectionM, 1, false, projectionM, 0);
	}

	private void addPoint(float x, float y, float z)
	{
		float sign = (bufi%3 == 0 ? 1.0f : -1.0f) * color;
		update[0] = update[4] = x;
		update[1] = update[5] = y;
		update[2] = update[6] = z;
		update[3] = sign; update[7] = -sign;
		glBufferSubData(GL_ARRAY_BUFFER, 2*4*Float.BYTES + bufi*Float.BYTES, 8*Float.BYTES, FloatBuffer.wrap(update));
		bufi += 8;
	}

	private void setCursor(float x, float y)
	{
		/* FIXME remove constants !!! */
		cursq[0] = cursq[4] = x-10.0f;
		cursq[2] = cursq[6] = x+10.0f;
		cursq[1] = cursq[7] = y-10.0f;
		cursq[3] = cursq[5] = y+10.0f;

		glBufferSubData(GL_ARRAY_BUFFER, 0, Float.BYTES*8, FloatBuffer.wrap(cursq));
		glUseProgram(p_cursor);
	}

	private int raiseShader(String path) throws IOException, FileNotFoundException, RuntimeException
	{
		int type = path.endsWith(".vert") ? GL_VERTEX_SHADER : path.endsWith(".frag") ? GL_FRAGMENT_SHADER : -1;
		if (type < 0)
		{
			throw new FileNotFoundException("Set up file extension.");
		}
		else
		{
			int id = glCreateShader(type);
			glShaderSource(id, file2String(path));
			glCompileShader(id);
			if (compileError(id))
			{
				throw new RuntimeException(
						String.format("%s compilation error. See log: ", path) +
						glGetShaderInfoLog(id)
					);
			}
			return id;
		}
	}

	private boolean compileError(int shader)
	{
		int[] success = new int[1];
		glGetShaderiv(shader, GL_COMPILE_STATUS, success, 0);
		return success[0] == GL_FALSE;
	}

	private String file2String(String path) throws IOException
	{
		StringBuilder sourcecode = new StringBuilder();
		InputStream stream = this.getContext().getAssets().open(path);

		while (stream.available() > 0)
		{
			sourcecode.append((char) stream.read());
		}
		stream.close();

		return sourcecode.toString();
	}

	public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display)
	{
		int attribs[] = {
			EGL_LEVEL, 0, /* default display frame buffer */
			EGL_RENDERABLE_TYPE, 4, /* ES 2.0 */

			EGL_COLOR_BUFFER_TYPE, EGL_RGB_BUFFER,
			EGL_RED_SIZE, 8,
			EGL_GREEN_SIZE, 8,
			EGL_BLUE_SIZE, 8,
			EGL_DEPTH_SIZE, 16,

			EGL_SAMPLE_BUFFERS, 1,
			EGL_SAMPLES, 4,  // 4x MSAA.

			EGL_NONE /* attribute list terminator */
		};

		EGLConfig[] configs = new EGLConfig[1];
		int[] configCounts = new int[1];
		egl.eglChooseConfig(display, attribs, configs, 1, configCounts);
		return configs[0];
	}
}
