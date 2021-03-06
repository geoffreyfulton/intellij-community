// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.vcs.log.ui.frame;

import com.intellij.ide.ui.laf.intellij.MacIntelliJProgressBarUI;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.ui.JBColor.namedColor;

public abstract class ProgressStripeIcon implements Icon {
  private static final int TRANSLATE = 1;
  @NotNull
  private final JComponent myReferenceComponent;
  private final int myShift;

  private ProgressStripeIcon(@NotNull JComponent component, int shift) {
    myReferenceComponent = component;
    myShift = shift;
  }

  public abstract int getChunkWidth();

  protected abstract void paint(@NotNull Graphics2D g2, int x, int y, int shift);

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
    Graphics2D g2 = (Graphics2D)g;

    int shift = myShift - getChunkWidth();
    while (shift < getIconWidth()) {
      paint(g2, x, y, shift);
      shift += getChunkWidth();
    }

    config.restore();
  }

  @Override
  public int getIconWidth() {
    return myReferenceComponent.getWidth();
  }

  private static class GradientIcon extends ProgressStripeIcon {
    private static final Color DARK = new ProgressStripeColor(namedColor("ProgressBar.indeterminateStartColor",
                                                                         new JBColor(new Color(0x4d9ff8), new Color(0x6a6a6a))),
                                                              MacIntelliJProgressBarUI.GRAPHITE_START_COLOR);
    private static final Color LIGHT = new ProgressStripeColor(namedColor("ProgressBar.indeterminateEndColor",
                                                                          new JBColor(new Color(0x90c2f8), new Color(0x838383))),
                                                               MacIntelliJProgressBarUI.GRAPHITE_END_COLOR);
    private static final int GRADIENT = 128;
    private static final int GRADIENT_HEIGHT = 2;

    private GradientIcon(@NotNull JComponent component, int shift) {
      super(component, shift);
    }

    @Override
    public int getChunkWidth() {
      return 2 * JBUI.scale(GRADIENT);
    }

    @Override
    public void paint(@NotNull Graphics2D g2, int x, int y, int shift) {
      Color dark = DARK;
      Color light = LIGHT;
      g2.setPaint(new GradientPaint(x + shift, y, dark, x + shift + JBUI.scale(GRADIENT), y, light));
      g2.fill(new Rectangle(x + shift, y, JBUI.scale(GRADIENT), getIconHeight()));
      g2.setPaint(new GradientPaint(x + shift + JBUI.scale(GRADIENT), y, light, x + shift + 2 * JBUI.scale(GRADIENT), y, dark));
      g2.fill(new Rectangle(x + shift + JBUI.scale(GRADIENT), y, JBUI.scale(GRADIENT), getIconHeight()));
    }

    @Override
    public int getIconHeight() {
      return JBUI.scale(GRADIENT_HEIGHT);
    }

    private static class ProgressStripeColor extends JBColor {
      private ProgressStripeColor(@NotNull JBColor defaultColor, @NotNull Color graphiteColor) {
        super(() -> {
          if (UIUtil.isUnderAquaBasedLookAndFeel() && !UIUtil.isUnderDarcula() && UIUtil.isGraphite()) {
            return graphiteColor;
          }
          return defaultColor;
        });
      }
    }
  }

  @NotNull
  public static AsyncProcessIcon generateIcon(@NotNull JComponent component) {
    List<Icon> result = new ArrayList<>();
    for (int i = 0; i < 2 * JBUI.scale(GradientIcon.GRADIENT); i += JBUI.scale(TRANSLATE)) {
      result.add(new GradientIcon(component, i));
    }

    Icon passive = result.get(0);
    AsyncProcessIcon icon = new AsyncProcessIcon("ProgressStripeIcon", result.toArray(new Icon[0]), passive) {
      @Override
      public Dimension getPreferredSize() {
        return new Dimension(component.getWidth(), passive.getIconHeight());
      }
    };
    component.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        super.componentResized(e);
        icon.revalidate();
      }
    });
    return icon;
  }
}
