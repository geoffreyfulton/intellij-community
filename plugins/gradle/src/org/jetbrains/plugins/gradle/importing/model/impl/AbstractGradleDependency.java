package org.jetbrains.plugins.gradle.importing.model.impl;

import com.intellij.openapi.roots.DependencyScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.importing.model.GradleDependency;

import java.io.Serializable;

/**
 * @author Denis Zhdanov
 * @since 8/10/11 6:41 PM
 */
public abstract class AbstractGradleDependency implements GradleDependency, Serializable {

  private static final long serialVersionUID = 1L;
  
  private DependencyScope myScope = DependencyScope.COMPILE;
  private boolean myExported;
  
  @NotNull
  @Override
  public DependencyScope getScope() {
    return myScope;
  }

  public void setScope(DependencyScope scope) {
    myScope = scope;
  }

  @Override
  public boolean isExported() {
    return myExported;
  }

  public void setExported(boolean exported) {
    myExported = exported;
  }

  @Override
  public String toString() {
    return "scope: " + getScope() + ", exported: " + isExported();
  }
}
