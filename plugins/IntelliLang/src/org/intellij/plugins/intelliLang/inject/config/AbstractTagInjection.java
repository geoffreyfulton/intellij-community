/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.intelliLang.inject.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMExternalizer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import org.intellij.plugins.intelliLang.util.StringMatcher;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base class for XML-related injections (XML tags and attributes).
 * Contains the tag's local name and namespace-uri, an optional value-pattern
 * and an optional XPath expression (only valid if XPathView is installed) and
 * the appropriate logic to determine if a tag matches those properties.
 *
 * @see org.intellij.plugins.intelliLang.inject.config.XPathSupportProxy
 */
public abstract class AbstractTagInjection<T extends AbstractTagInjection, I extends PsiElement> extends BaseInjection<T, I> {

  @NotNull @NonNls
  private StringMatcher myTagName = StringMatcher.ANY;

  @NotNull @NonNls
  private Set<String> myTagNamespace = Collections.emptySet();

  @NotNull @NonNls
  private String myXPathCondition = "";
  private Object myCompiledXPathCondition;

  @NotNull
  public String getTagName() {
    return myTagName.getPattern();
  }

  public void setTagName(@NotNull @NonNls String tagName) {
    myTagName = StringMatcher.create(tagName);
  }

  @NotNull
  public String getTagNamespace() {
    return StringUtil.join(myTagNamespace, "|");
  }

  public void setTagNamespace(@NotNull @NonNls String tagNamespace) {
    myTagNamespace = new TreeSet<String>(StringUtil.split(tagNamespace,"|"));
  }

  @NotNull
  public String getXPathCondition() {
    return myXPathCondition;
  }

  @Nullable
  public XPath getCompiledXPathCondition() {
    if (isInvalid(myCompiledXPathCondition)) {
      return null;
    }
    else if (myCompiledXPathCondition != null) {
      return (XPath)myCompiledXPathCondition;
    }
    else if (myXPathCondition != null && myXPathCondition.length() > 0) {
      try {
        final XPathSupportProxy xPathSupport = XPathSupportProxy.getInstance();
        if (xPathSupport != null) {
          return (XPath)(myCompiledXPathCondition = xPathSupport.createXPath(myXPathCondition));
        }
        else {
          myCompiledXPathCondition = XPathSupportProxy.UNSUPPORTED;
        }
      }
      catch (JaxenException e) {
        myCompiledXPathCondition = XPathSupportProxy.INVALID;
        Logger.getInstance(getClass().getName()).info("Invalid XPath expression", e);
      }
    }
    return null;
  }

  private static boolean isInvalid(Object expr) {
    return expr == XPathSupportProxy.INVALID || expr == XPathSupportProxy.UNSUPPORTED;
  }

  public void setXPathCondition(@Nullable String condition) {
    myXPathCondition = condition != null ? condition : "";
    myCompiledXPathCondition = null;
  }

  @SuppressWarnings({"RedundantIfStatement"})
  protected boolean matches(@Nullable XmlTag tag) {
    if (tag == null) {
      return false;
    }
    if (!myTagName.matches(tag.getLocalName())) {
      return false;
    }
    if (!myTagNamespace.contains(tag.getNamespace())) {
      return false;
    }
    return true;
  }

  public void copyFrom(@NotNull T other) {
    super.copyFrom(other);
    myTagName = other.myTagName;
    myTagNamespace = other.myTagNamespace;
    setXPathCondition(other.getXPathCondition());
  }

  protected void readExternalImpl(Element e) {
    setTagName(JDOMExternalizer.readString(e, "TAGNAME"));
    setTagNamespace(JDOMExternalizer.readString(e, "TAGNAMESPACE"));
    setXPathCondition(JDOMExternalizer.readString(e, "XPATH_CONDITION"));
  }

  protected void writeExternalImpl(Element e) {
    JDOMExternalizer.write(e, "TAGNAME", getTagName());
    JDOMExternalizer.write(e, "TAGNAMESPACE", getTagNamespace());
    JDOMExternalizer.write(e, "XPATH_CONDITION", myXPathCondition);
  }

  @SuppressWarnings({"RedundantIfStatement"})
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    final AbstractTagInjection that = (AbstractTagInjection)o;

    if (!myTagName.equals(that.myTagName)) return false;
    if (!myTagNamespace.equals(that.myTagNamespace)) return false;
    if (!myXPathCondition.equals(that.myXPathCondition)) return false;

    return true;
  }

  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myTagName.hashCode();
    result = 31 * result + myTagNamespace.hashCode();
    result = 31 * result + myXPathCondition.hashCode();
    return result;
  }

  protected boolean matchXPath(XmlElement context) {
    final XPath condition = getCompiledXPathCondition();
    if (condition != null) {
      try {
        return condition.booleanValueOf(context);
      }
      catch (JaxenException e) {
        myCompiledXPathCondition = XPathSupportProxy.INVALID;
        return false;
      }
    }
    return myXPathCondition.length() == 0;
  }
}
