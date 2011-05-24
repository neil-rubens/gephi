/*
Copyright 2008-2011 Gephi
Authors : Antonio Patriarca <antoniopatriarca@gmail.com>
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.gephi.visualization.view;

import com.jogamp.opengl.util.FPSAnimator;
import java.awt.Component;
import java.awt.Dimension;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import org.gephi.visualization.controller.Controller;
import org.gephi.visualization.data.FrameData;
import org.gephi.visualization.data.FrameDataBridgeOut;
import org.gephi.visualization.pipeline.Pipeline;
import org.gephi.visualization.pipeline.gl11.GL11EdgesLayout3D;
import org.gephi.visualization.pipeline.gl11.GL11NodesLayout3D;
import org.gephi.visualization.pipeline.gl11.GL11Pipeline3D;

/**
 * Class which controls the rendering loop.
 *
 * @author Antonio Patriarca <antoniopatriarca@gmail.com>
 */
public class View implements GLEventListener {

    private GLCanvas canvas;
    private FPSAnimator animator;

    final private Controller controller;
    final private FrameDataBridgeOut bridge;

    private FrameData frameData = null;

    private Pipeline pipeline;

    private int countNulls = 0;

    static { GLProfile.initSingleton(true); }

    public View(Controller controller, FrameDataBridgeOut bridge) {
        this.controller = controller;
        this.bridge = bridge;

        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        caps.setDoubleBuffered(true);
        caps.setHardwareAccelerated(true);
        // TODO: change capabilities based on config files

        this.canvas = new GLCanvas(caps);
        this.canvas.setAutoSwapBufferMode(true);
        this.animator = new FPSAnimator(this.canvas, 30);

        this.canvas.addKeyListener(controller);
	this.canvas.addMouseListener(controller);
        this.canvas.addMouseMotionListener(controller);
        this.canvas.addMouseWheelListener(controller);

        this.pipeline = new GL11Pipeline3D();
        this.bridge.setLayout(new GL11NodesLayout3D());
        this.bridge.setLayout(new GL11EdgesLayout3D());
    }
    
    public Component getCanvas() {
        return this.canvas;
    }

    public void start() {
        this.canvas.addGLEventListener(this);
        this.canvas.setVisible(true);
        this.animator.start();
    }

    public void stop() {
        this.animator.stop();
        this.canvas.setVisible(false);
        this.canvas.removeGLEventListener(this);
    }

    @Override
    public void init(GLAutoDrawable glad) {
        final GL gl = glad.getGL();
        gl.setSwapInterval(1);

        // TODO: change initialization code based on config files

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClearDepth(1.0);

        gl.glEnable(GL.GL_DEPTH_TEST);

        boolean init = this.pipeline.init(gl);

        if (!init) {
            gl.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        }
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        GL gl = glad.getGL();

        this.pipeline.dispose(gl);
    }

    @Override
    public void display(GLAutoDrawable glad) {
        final GL gl = glad.getGL();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        this.frameData = this.bridge.updateCurrent();

        if (this.frameData == null) {
            ++countNulls;
            return;
        }

        this.controller.beginRenderFrame();

        this.pipeline.draw(gl, frameData);

        this.controller.endRenderFrame();

        gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        final GL gl = glad.getGL();

        int h2 = h == 0 ? 1 : h;

        gl.glViewport(0, 0, w, h2);
    }

    public Dimension getDimension() {
        return this.canvas.getSize();
    }

    public void updateSize(int x, int y, int w, int h) {
        this.canvas.setBounds(x, y, w, h);
        this.controller.resize(w, h);
    }
    
}