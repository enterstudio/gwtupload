/*
 * Copyright 2010 Manuel Carrasco Moñino. (manolo at apache/org)
 * http://code.google.com/p/gwtupload
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package gwtupload.client;

import java.util.HashMap;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A widget which wraps a FileUpload (native input file), hidding it
 * and replacing it by any clickable, customizable and stylable Widget.
 *
 * To use it, first attach any HasClickHandlers widget to your display,
 * then wrap it with the DecoratedFileUpload.
 *
 * <pre>

  Button myButton = new Button("Upload a file");
  RootPanel.get().add(myButton);

  DecoratedFileUpload d = new DecoratedFileUpload(myButton);

 * </pre>
 *
 * To use it in UiBinder
 *
 * <pre>

     &lt;up:DecoratedFileUpload&gt;
        &lt;g:Button&gt;Select a file ...&lt;/g:Button&gt;
     &lt;/up:DecoratedFileUpload&gt;

 * </pre>
 *
 * CSS Rules:
 *
 * <pre>
.DecoratedFileUpload {
  margin-right: 5px;
}

.DecoratedFileUpload-button {
  white-space: nowrap;
  font-size: 10px;
  min-height: 15px;
}

.DecoratedFileUpload .gwt-Anchor,
.DecoratedFileUpload .gwt-Label {
  color: blue;
  text-decoration: underline;
  cursor: pointer;
}

.DecoratedFileUpload-button:HOVER,
.DecoratedFileUpload .gwt-Button-over,
.DecoratedFileUpload .gwt-Anchor-over,
.DecoratedFileUpload .gwt-Label-over {
  color: #af6b29;
}

.DecoratedFileUpload-disabled .gwt-Button,
.DecoratedFileUpload-disabled .gwt-Anchor,
.DecoratedFileUpload-disabled .gwt-Label {
  color: grey;
}
 * </pre>
 *
 * @author Manuel Carrasco Moñino
 *
 */
public class DecoratedFileUpload extends FlowPanel implements HasName, HasChangeHandlers {

  /**
   * A FileUpload which implements onChange, onMouseOver and onMouseOut events.
   *
   * Note: although FileUpload implements HasChangeHandlers and setEnabled in version Gwt 2.0.x,
   * we put it here in order to be compatible with older Gwt versions.
   *
   */
  public static class FileUploadWithMouseEvents extends MultipleFileUpload implements HasMouseOverHandlers, HasMouseOutHandlers, HasChangeHandlers {

    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
      return addDomHandler(handler, ChangeEvent.getType());
    }

    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
      return addDomHandler(handler, MouseOutEvent.getType());
    }

    public HandlerRegistration addMouseOverHandler(final MouseOverHandler handler) {
      return addDomHandler(handler, MouseOverEvent.getType());
    }

    public boolean isEnabled() {
      return !getElement().getPropertyBoolean("disabled");
    }

    public void setEnabled(boolean enabled) {
      getElement().setPropertyBoolean("disabled", !enabled);
    }
  }

  /**
   * An abstract class which is the base for specific browser implementations.
   */
  private abstract static class DecoratedFileUploadImpl {

    protected Widget button;
    protected Panel container;
    protected FileUploadWithMouseEvents input;

    public void init(Panel container, FileUploadWithMouseEvents input) {
      this.container = container;
      this.input = input;
    }

    public void setButton(Widget widget) {
      this.button = widget;
      if (button instanceof HasMouseOverHandlers) {
        ((HasMouseOverHandlers) button).addMouseOverHandler(new MouseOverHandler() {
          public void onMouseOver(MouseOverEvent event) {
            button.addStyleName(STYLE_CLICKABLE_WIDGET + "-" + STYLE_BUTTON_OVER_SUFFIX);
            container.addStyleDependentName(STYLE_BUTTON_OVER_SUFFIX);
          }
        });
      }
      if (button instanceof HasMouseOutHandlers) {
        ((HasMouseOutHandlers) button).addMouseOutHandler(new MouseOutHandler() {
          public void onMouseOut(MouseOutEvent event) {
            button.removeStyleName(STYLE_CLICKABLE_WIDGET + "-" + STYLE_BUTTON_OVER_SUFFIX);
            container.removeStyleDependentName(STYLE_BUTTON_OVER_SUFFIX);
          }
        });
      }
    }

    public void onAttach() {
    }

    public void resize() {
    }
  }

  /**
   * Implementation for browsers which support the click() method:
   * IE, Chrome, Safari
   *
   * The hack here is to put the customized button
   * and the file fileUplad statically positioned in an absolute panel.
   * This panel has the size of the button, and the fileUplad is not shown
   * because it is placed out of the width and height panel limits.
   *
   */
  private static class DecoratedFileUploadImplClick extends DecoratedFileUploadImpl {

    private static HashMap<Widget, HandlerRegistration> clickHandlerCache = new HashMap<Widget, HandlerRegistration>();

    private static native void clickOnInputFile(Element elem) /*-{
      elem.click();
    }-*/;

    public void init(Panel container, FileUploadWithMouseEvents input) {
      super.init(container, input);
      container.add(input);
      DOM.setStyleAttribute(input.getElement(), "position", "fixed");
      DOM.setStyleAttribute(input.getElement(), "display", "inline");
      DOM.setStyleAttribute(input.getElement(), "top", "-1000px");
      DOM.setStyleAttribute(input.getElement(), "left", "-1000px");
    }

    public void setButton(Widget widget) {
      super.setButton(widget);
      HandlerRegistration clickRegistration = clickHandlerCache.get(widget);
      if (clickRegistration != null) {
        clickRegistration.removeHandler();
      }
      if (button != null) {
        if (button instanceof HasClickHandlers) {
          clickRegistration = ((HasClickHandlers) button).addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
              clickOnInputFile(input.getElement());
            }
          });
          clickHandlerCache.put(widget, clickRegistration);
        }
      }
    }
  }

  /**
   * Implementation for IE6-8
   */
  @SuppressWarnings("unused")
  private static class DecoratedFileUploadImplIE extends DecoratedFileUploadImplClick {
    public void init(Panel container, FileUploadWithMouseEvents input) {
      super.init(container, input);
      DOM.setStyleAttribute(input.getElement(), "position", "absolute");
    }
  }

  /**
   * Implementation for browsers which do not support the click() method:
   * FF, Opera
   *
   * The hack here is to place the customized button and the file fileUplad positioned
   * statically in an absolute panel which has size of the button.
   * The file fileUplad is wrapped into a transparent panel, which also has the button
   * size and is placed covering the customizable button.
   *
   * When the user puts his mouse over the button and clicks on it, what really
   * happens is that the user clicks on the transparent file fileUplad showing
   * the choose file dialog.
   *
   */
  @SuppressWarnings("unused")
  private static class DecoratedFileUploadImplNoClick extends DecoratedFileUploadImpl {

    private static final int DEFAULT_HEIGHT = 15;
    private static final int DEFAULT_WIDTH = 100;

    private SimplePanel wrapper;

    public void init(Panel container, FileUploadWithMouseEvents input) {
      super.init(container, input);
      wrapper = new SimplePanel();
      wrapper.add(input);
      container.add(wrapper);
      wrapper.setStyleName("wrapper");

      // Not using the GWT 2.0.x way to set Style attributes in order to be
      // compatible with old GWT releases
      DOM.setStyleAttribute(container.getElement(), "position", "relative");
      DOM.setStyleAttribute(container.getElement(), "overflow", "hidden");

      DOM.setStyleAttribute(wrapper.getElement(), "position", "absolute");
      DOM.setStyleAttribute(wrapper.getElement(), "textAlign", "left");
      DOM.setStyleAttribute(wrapper.getElement(), "zIndex", "1");
      DOM.setStyleAttribute(input.getElement(), "marginLeft", "-1500px");
      DOM.setStyleAttribute(input.getElement(), "fontSize", "350px");
      DOM.setStyleAttribute(input.getElement(), "borderWidth", "0px");
      DOM.setStyleAttribute(input.getElement(), "opacity", "0");
      DOM.setElementAttribute(input.getElement(), "size", "1");
      DOM.setElementAttribute(input.getElement(), "cursor", "pointer");

      // Trigger over and out handlers which already exist in the covered button.
      input.addMouseOverHandler(new MouseOverHandler() {
        public void onMouseOver(MouseOverEvent event) {
          if (button != null) {
            button.fireEvent(event);
          }
        }
      });
      input.addMouseOutHandler(new MouseOutHandler() {
        public void onMouseOut(MouseOutEvent event) {
          if (button != null) {
            button.fireEvent(event);
          }
        }
      });
    }

    public void onAttach() {
      if (width != 0 && height != 0) {
        container.setSize(width + "px", height + "px");
      } else {
        resize();
      }
      wrapper.setSize(width + "px", height + "px");
    }

    protected int width = 0, height = 0;

    // TODO: computed size
    public void resize() {
      if (button != null) {
        DOM.setStyleAttribute(button.getElement(), "position", "absolute");

        int w = button.getElement().getOffsetWidth();
        int h = button.getElement().getOffsetHeight();
        if (w <= 0) {
          // Using old way for compatibility
          String ws = DOM.getStyleAttribute(button.getElement(), "width");
          if (ws != null) {
            try {
              w = Integer.parseInt(ws.replaceAll("[^\\d]", ""));
            } catch (Exception e) {
            }
          }
          if (w <= 0) {
            w = DEFAULT_WIDTH;
          } else {
            width = w;
          }
        }
        if (h <= 0) {
          // Using old way for compatibility
          String hs = DOM.getStyleAttribute(button.getElement(), "height");
          if (hs != null) {
            try {
              h = Integer.parseInt(hs.replaceAll("[^\\d]", ""));
            } catch (Exception e) {
            }
          }
          if (h <= 0) {
            h = DEFAULT_HEIGHT;
          } else {
            height = h;
          }
        }
        container.setSize(w + "px", h + "px");
      }

      wrapper.setSize(width + "px", height + "px");
    }

    public void setSize(String width, String height) {
      button.setSize(width, height);
      container.setSize(width, height);
      wrapper.setSize(width, height);
    }
  }

  private static final String STYLE_BUTTON_OVER_SUFFIX = "over";
  private static final String STYLE_CONTAINER = "DecoratedFileUpload";
  private static final String STYLE_CLICKABLE_WIDGET = "DecoratedFileUpload-button";
  private static final String STYLE_DISABLED_SUFFIX = "disabled";
  protected Widget button;
  protected FileUploadWithMouseEvents input;;
  protected boolean reuseButton = false;
  private DecoratedFileUploadImpl impl;
  private String text = "";

  /**
   * Default constructor.
   */
  public DecoratedFileUpload() {
    this(null, null);
  }


  /**
   * Set the text when the element is attached.
   */
  public DecoratedFileUpload(String text) {
    this((Widget)null);
    this.text = text;
  }

  /**
   * Constructor which uses the provided widget as the button where the
   * user has to click to show the browse file dialog.
   * The widget has to implement the HasClickHandlers interface.
   */
  public DecoratedFileUpload(Widget button) {
    this(button, null);
  }

  public DecoratedFileUpload(Widget button, FileUploadWithMouseEvents in) {
    impl = GWT.create(DecoratedFileUploadImpl.class);
    this.addStyleName(STYLE_CONTAINER);
    input = in;
    if (input == null) {
      input = new FileUploadWithMouseEvents();
    }
    impl.init(this, input);
    setButton(button);
  }

  /**
   * Add a handler which will be fired when the user selects a file.
   */
  public HandlerRegistration addChangeHandler(ChangeHandler handler) {
    return input.addChangeHandler(handler);
  }

  /**
   * Return the file name selected by the user.
   */
  public String getFilename() {
    return input.getFilename();
  }

  /**
   * Return the file names selected by the user.
   */
  public List<String> getFilenames() {
	  return input.getFilenames();
  }

  /**
   * Return the original FileUpload wrapped by this decorated widget.
   */
  public FileUpload getFileUpload() {
    return input;
  }

  /**
   * Return the name of the widget.
   */
  public String getName() {
    return input.getName();
  }

  /**
   * Return the text shown in the clickable button.
   */
  public String getText() {
    return text;
  }

  /**
   * Return this widget instance.
   */
  public Widget getWidget() {
    return this;
  }

  /**
   * Return whether the fileUplad is enabled.
   */
  public boolean isEnabled() {
    return input.isEnabled();
  }

  /* (non-Javadoc)
   * @see com.google.gwt.user.client.ui.Composite#onAttach()
   */
  @Override
  public void onAttach() {
    super.onAttach();
    if (button == null) {
      button = new Label(text);
      setButton(button);
    }
    new Timer(){
      public void run() {
        impl.onAttach();
      }
    }.schedule(5);
  }

  /**
   * Set the button the user has to click on to show the browse dialog.
   */
  public void setButton(Widget button) {
    if (button != null) {
      assert button instanceof HasClickHandlers : "Button should implement HasClickHandlers";
      if (this.button != null) {
        this.remove(this.button);
      }
      this.button = button;
      super.add(button);
      impl.setButton(button);
      button.addStyleName(STYLE_CLICKABLE_WIDGET);
      updateSize();
    }
  }

  /**
   * Set the button size.
   */
  public void setButtonSize(String width, String height) {
    button.setSize(width, height);
    updateSize();
  }

  /**
   * Enable or disable the FileInput.
   */
  public void setEnabled(boolean b) {
    input.setEnabled(b);
    if (b) {
      this.removeStyleDependentName(STYLE_DISABLED_SUFFIX);
    } else {
      this.addStyleDependentName(STYLE_DISABLED_SUFFIX);
    }
  }

  /**
   * Set the widget name.
   */
  public void setName(String fieldName) {
    input.setName(fieldName);
  }

  /* (non-Javadoc)
   * @see com.google.gwt.user.client.ui.UIObject#setSize(java.lang.String, java.lang.String)
   */
  public void setSize(String width, String height){
    setButtonSize(width, height);
  }

  /**
   * Set the text of the button.
   */
  public void setText(String text) {
    this.text = text;
    if (button instanceof HasText) {
      ((HasText) button).setText(text);
      updateSize();
    }
  }

  /**
   * Resize the absolute container to match the button size.
   */
  public void updateSize() {
    impl.resize();
  }

  public void enableMultiple(boolean b) {
    input.enableMultiple(b);
  }

  public void setAccept(String s) {
    input.setAccept(s);
  }

  @Override
  public void add(Widget widget) {
    // Be compatible with UIBinder (#179), but avoid adding our own FileInput (#205)
    if (widget instanceof HasClickHandlers && !(widget instanceof FileUploadWithMouseEvents)) {
      setButton(widget);
    } else {
      super.add(widget);
    }
  }

}
