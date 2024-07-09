package newpson.ajournal;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.AttributeSet;
import android.util.Log;

import android.opengl.GLSurfaceView;
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

/* @WARN OpenGL ES 3.0 is required because of GL_PRIMITIVE_RESTART_FIXED_INDEX */
public class WacomSurface extends GLSurfaceView implements GLSurfaceView.Renderer, GLSurfaceView.EGLConfigChooser
{
	// public static final int MAX_MEMORY_RATE = 65536; /* 65 KB too few because of high stroke resolution */ 
	public static final int MAX_MEMORY_RATE = 1572864; /* 1.5 MB is OK */
	/* FIXME constants everywhere... */
	public static final float WACOM_WIDTH = 15200f;
	public static final float WACOM_HEIGHT = 9500f;
	public static final float WACOM_SENSITIVITY = 2048f;
	private boolean showCursor = true;
	// public static final int MAX_MEMORY_RATE = 33554432; /* 32 MB too many (laggy draw) */

	/**
	 * sv_ - vertex shader
	 * sf_ - fragment shader
	 * p_ - program
	 * b_ - buffer 
	 * u_ - uniform 
	 * a_ - attribute
	 */
	private int p_stroke = 0;
	private int sv_stroke = 0;
	private int sf_stroke = 0;

	private int p_cursor = 0;
	private int sv_cursor = 0;
	private int sf_cursor = 0;

	private int b_array = 0;
	// private int u_surface = 0;
	// private int u_pos = 0;
	private int a_pos = 0; /* manual indexing is @IMPORTANT !!! 4 hours, my dudes ... */
	private int a_prev = 1;
	private int a_cur = 2;
	private int a_next = 3;

	FloatBuffer update;
	FloatBuffer cursq; /* cursor square */
	private int bufi = 0;
	private int vertc = 0;
	
    public WacomSurface(Context context, AttributeSet attrs)
    {
		super(context, attrs);

		setEGLConfigChooser(this);
		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		update = FloatBuffer.allocate(4*2);
		cursq = FloatBuffer.allocate(2*4);
    }

	public int getMemoryUsage()
	{
		return 8*4 + vertc*4*4;
	}

	public void dragStart(final int x, final int y)
	{
		queueEvent(new Runnable() {
			@Override
        	public void run() {
				addPoint((float) x, (float) y, 0.0f); /* prev */
				addPoint((float) x, (float) y, 0.0f); /* cur */
				vertc += 4;
        	}
        });
	}

	public void move(final int x, final int y)
	{
		queueEvent(new Runnable() {
			@Override
    		public void run() {
				setCursor((float) x, (float) y);
    		}
    	});
		requestRender();
	}

	public void drag(final int x, final int y, final int pressure)
	{
		queueEvent(new Runnable() {
			@Override
    		public void run() {
				addPoint((float) x, (float) y, (float) pressure); /* cur */
				addPoint((float) x, (float) y, (float) pressure); /* next */
				bufi -= 8; /* modify cur */
				vertc += 2;
    		}
    	});
		requestRender();
	}

	public void dragStop(final int x, final int y)
	{
		queueEvent(new Runnable() {
			@Override
        	public void run() {
				addPoint((float) x, (float) y, 0.0f);
				addPoint((float) x, (float) y, 0.0f);
				vertc += 4;
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
		glBufferData(GL_ARRAY_BUFFER, 4*8+MAX_MEMORY_RATE, null, GL_DYNAMIC_DRAW); /* + cursor square at very beginning */
		
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
		glVertexAttribPointer(a_prev, 4, GL_FLOAT, false, 4*4, 0);
		glVertexAttribPointer(a_cur, 4, GL_FLOAT, false, 4*4, 32);
		glVertexAttribPointer(a_next, 4, GL_FLOAT, false, 4*4, 64);
		glEnableVertexAttribArray(a_prev);
		glEnableVertexAttribArray(a_cur);
		glEnableVertexAttribArray(a_next);
		glLinkProgram(p_stroke);

		/* .-= CURSOR PROGRAM =-. */
		p_cursor = glCreateProgram();
		glAttachShader(p_cursor, sv_cursor);
		glAttachShader(p_cursor, sf_cursor);

		glBindAttribLocation(p_cursor, a_pos, "in_pos");
		glVertexAttribPointer(a_pos, 2, GL_FLOAT, false, 4*2, 0);
		glEnableVertexAttribArray(a_pos);
		glLinkProgram(p_cursor);
		// u_surface = glGetUniformLocation(p_cursor, "surface"); /* @IMPORTANT get location only after linking! */
		// u_pos = glGetUniformLocation(p_cursor, "pos");

		/* .-= SURFACE SETTINGS =-.*/
		// setEnvironment(15200f, 9500f, 2048f);
		glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	public void onDrawFrame(GL10 unused)
	{
		glClear(GL_COLOR_BUFFER_BIT);

		glUseProgram(p_stroke);
		glDrawArrays(GL_TRIANGLE_STRIP, 2 /* skip 4*8 bytes */, vertc-4);

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

	public void onSurfaceChanged(GL10 unused, int width, int height)
	{
		glViewport(0, 0, width, height);
		// glUseProgram(p_cursor);  /* @IMPORTANT uniform loading requires program in use */
		// glUniform2f(u_surface, (float) width, (float) height);
		Log.e("ajournal-debug", String.format("Surface size: %dx%d, err(%d)", width, height, glGetError()));
	}

	private void addPoint(float x, float y, float z)
	{
		float sign = bufi%3 == 0 ? 1.0f : -1.0f;
		update.clear();
		update.put(x);
		update.put(y);
		update.put(z);
		update.put(sign);
		update.put(x);
		update.put(y);
		update.put(z);
		update.put(-sign);
		update.flip();
		/* @WARN all offsets in bytes!!! !!! */
		glBufferSubData(GL_ARRAY_BUFFER, 2*4*4 + bufi*4, 8*4, update);
		bufi += 8;
	}

	private void setCursor(float x, float y)
	{
		cursq.clear();
		/* FIXME remove constants !!! */
		cursq.put(x-50f);
		cursq.put(y-50f);
		cursq.put(x+50f);
		cursq.put(y+50f);
		cursq.put(x-50f);
		cursq.put(y+50f);
		cursq.put(x+50f);
		cursq.put(y-50f);
		cursq.flip();
		glBufferSubData(GL_ARRAY_BUFFER, 0, 4*8, cursq);
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

		/* bruh */
		return sourcecode.toString()
			.replace("${WACOM_WIDTH}", String.valueOf(WACOM_WIDTH))
			.replace("${WACOM_HEIGHT}", String.valueOf(WACOM_HEIGHT))
			.replace("${WACOM_SENSITIVITY}", String.valueOf(WACOM_SENSITIVITY));
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
