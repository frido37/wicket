/*
 * $Id: WicketTagComponentResolver.java,v 1.4 2005/01/18 08:04:29 jonathanlocke
 * Exp $ $Revision$ $Date$
 * 
 * ==============================================================================
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
package wicket.markup.html;

import wicket.Component;
import wicket.IComponentResolver;
import wicket.MarkupContainer;
import wicket.markup.ComponentTag;
import wicket.markup.MarkupException;
import wicket.markup.MarkupStream;
import wicket.markup.WicketTag;
import wicket.markup.html.basic.Label;
import wicket.markup.parser.XmlTag;
import wicket.markup.parser.filter.WicketMessageTagHandler;

/**
 * This is a tag resolver which handles &lt;wicket:messae&gt; tags. The looks
 * like &lt;wicket:message attr="myKey"&gt;Default Text&lt;/wicket:message&gt;
 * The resolver will replace the whole tag with the message found in the 
 * properties file associated with the parent markup container. If not
 * message is found, the default body text will remain. 
 * 
 * @author Juergen Donnerstag
 */
public class WicketMessageResolver implements IComponentResolver
{
	private static final long serialVersionUID = 1L;

	/**
	 * Try to resolve the tag, then create a component, add it to the container
	 * and render it.
	 * 
	 * @see wicket.IComponentResolver#resolve(MarkupContainer, MarkupStream,
	 *      ComponentTag)
	 * 
	 * @param container
	 *            The container parsing its markup
	 * @param markupStream
	 *            The current markupStream
	 * @param tag
	 *            The current component tag while parsing the markup
	 * @return true, if componentId was handle by the resolver. False, otherwise
	 */
	public boolean resolve(final MarkupContainer container, final MarkupStream markupStream,
			final ComponentTag tag)
	{
		// A global switch to enable / disable wicket:message
		if (WicketMessageTagHandler.enable == false)
		{
			return false;
		}
		
		// It must be <body onload>
		if (tag instanceof WicketTag)
		{
			WicketTag wtag = (WicketTag) tag;
			if (wtag.isMessageTag() && (wtag.getNamespace() != null))
			{
				String messageKey = wtag.getAttributes().getString("key");
				if ((messageKey == null) || (messageKey.trim().length() == 0))
				{
					throw new MarkupException(
							"Wrong format of <wicket:message key='xxx'>: attribute 'key' is missing");
				}

				final String value = container.getApplication().getLocalizer().getString(
						messageKey, container, "");

				final String id = "_message_" + container.getPage().getAutoIndex();
				Component component = null;
				if ((value != null) && (value.trim().length() > 0))
				{
					component = new MyLabel(id, value);
				}
				else
				{
					component = new WebMarkupContainer(id);
				}

				component.setRenderBodyOnly(container.getApplicationSettings().getStripWicketTags());

				container.autoAdd(component);

				// Yes, we handled the tag
				return true;
			}
		}

		// We were not able to handle the tag
		return false;
	}

	/**
	 * A Label with expands open-close tags to open-body-close if required
	 */
	public static class MyLabel extends Label
	{
		/**
		 * Construct.
		 * @param id
		 * @param value
		 */
		public MyLabel(final String id, final String value)
		{
			super(id, value);
		}
		private static final long serialVersionUID = 1L;
		
	    /**
	     * Renders this component.
	     */
	    protected final void onRender()
	    {
	        // Render the tag that included this html compoment
	        final MarkupStream markupStream = findMarkupStream();

	        final boolean atOpenTag = markupStream.atOpenTag();
	        if (atOpenTag)
	        {
	        	super.onRender();
	        }
	        else
	        {
		        // Open up the tag, change it from open-close to open, if necessary
		        final ComponentTag openTag = markupStream.getTag().mutable();
		        openTag.setType(XmlTag.OPEN);
		        if (getRenderBodyOnly() == false)
		        {
		            renderComponentTag(openTag);
		        }
	
				// Write the new body
		        String value = getModelObjectAsString();
				getResponse().write(value);
	
		        if (getRenderBodyOnly() == false)
		        {
		        	getResponse().write(openTag.syntheticCloseTagString());
		        }
	
		        // Skip opening tag
				markupStream.skipComponent();
		    }
	    }
	}
}