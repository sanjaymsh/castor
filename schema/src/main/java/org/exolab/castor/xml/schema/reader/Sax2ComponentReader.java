/**
 * Redistribution and use of this software and associated documentation ("Software"), with or
 * without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright statements and notices. Redistributions
 * must also contain a copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. The name "Exolab" must not be used to endorse or promote products derived from this Software
 * without prior written permission of Intalio, Inc. For written permission, please contact
 * info@exolab.org.
 *
 * 4. Products derived from this Software may not be called "Exolab" nor may "Exolab" appear in
 * their names without prior written permission of Intalio, Inc. Exolab is a registered trademark of
 * Intalio, Inc.
 *
 * 5. Due credit should be given to the Exolab Project (http://www.exolab.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY INTALIO, INC. AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL INTALIO, INC. OR ITS
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 1999-2002 (C) Intalio, Inc. All Rights Reserved.
 *
 * $Id$
 */

package org.exolab.castor.xml.schema.reader;

import org.exolab.castor.xml.AttributeSet;
import org.exolab.castor.xml.NamespacesStack;
import org.exolab.castor.xml.XMLException;
import org.exolab.castor.xml.util.AttributeSetImpl;
import org.xml.sax.AttributeList;
import org.xml.sax.DocumentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A SAX adapter class for the ComponentReader.
 * 
 * @author <a href="mailto:kvisco@intalio.com">Keith Visco</a>
 * @version $Revision$ $Date: 2006-04-14 04:14:43 -0600 (Fri, 14 Apr 2006) $
 **/
@SuppressWarnings("deprecation")
public final class Sax2ComponentReader implements DocumentHandler, org.xml.sax.ErrorHandler {

  private static final String XMLNS = "xmlns";
  private static final String XMLNS_PREFIX = XMLNS + ":";
  private static final String XML_PREFIX = "xml";

  private ComponentReader componentReader = null;

  /**
   * Represents the namespaces stack.
   */
  private NamespacesStack namespacesStack = null;

  public Sax2ComponentReader(ComponentReader compReader) {
    super();
    componentReader = compReader;
    namespacesStack = new NamespacesStack();
  }

  /**
   * Processes the attributes and namespace declarations found in the given SAX AttributeList. The
   * global AttributeSet is cleared and updated with the attributes. Namespace declarations are
   * added to the set of namespaces in scope.
   * 
   * @param atts the AttributeList to process.
   **/
  private AttributeSet processAttributeList(AttributeList atts) throws SAXException {

    if (atts == null)
      return new AttributeSetImpl(0);

    // -- process all namespaces first
    int attCount = 0;
    boolean[] validAtts = new boolean[atts.getLength()];
    for (int i = 0; i < validAtts.length; i++) {
      String attName = atts.getName(i);
      if (attName.equals(XMLNS)) {
        namespacesStack.addNamespace("", atts.getValue(i));
      } else if (attName.startsWith(XMLNS_PREFIX)) {
        String prefix = attName.substring(XMLNS_PREFIX.length());
        namespacesStack.addNamespace(prefix, atts.getValue(i));
      } else {
        validAtts[i] = true;
        ++attCount;
      }
    }
    // -- process validAtts...if any exist
    AttributeSetImpl attSet = null;
    if (attCount > 0) {
      attSet = new AttributeSetImpl(attCount);
      for (int i = 0; i < validAtts.length; i++) {
        if (!validAtts[i])
          continue;
        String namespace = null;
        String attName = atts.getName(i);
        int idx = attName.indexOf(':');
        if (idx > 0) {
          String prefix = attName.substring(0, idx);
          if (!prefix.equals(XML_PREFIX)) {
            attName = attName.substring(idx + 1);
            namespace = namespacesStack.getNamespaceURI(prefix);
            if (namespace == null) {
              String error = "The namespace associated with " + "the prefix '" + prefix
                  + "' could not be resolved.";
              throw new SAXException(error);

            }
          }
        }
        attSet.setAttribute(attName, atts.getValue(i), namespace);
      }
    } else
      attSet = new AttributeSetImpl(0);

    return attSet;

  }

  public void characters(char[] ch, int start, int length) throws org.xml.sax.SAXException {
    try {
      componentReader.characters(ch, start, length);
    } catch (XMLException ex) {
      throw new SAXException(ex);
    }

  }

  public void endDocument() throws org.xml.sax.SAXException {
    // -- do nothing
  }

  public void endElement(String name) throws org.xml.sax.SAXException {
    String namespace = null;
    int idx = name.indexOf(':');
    if (idx >= 0) {
      String prefix = name.substring(0, idx);
      name = name.substring(idx + 1);
      namespace = namespacesStack.getNamespaceURI(prefix);
    } else
      namespace = namespacesStack.getDefaultNamespaceURI();

    // remove namespaces
    namespacesStack.removeNamespaceScope();

    try {
      componentReader.endElement(name, namespace);
    } catch (XMLException ex) {
      throw new SAXException(ex);
    }
  }

  public void ignorableWhitespace(char[] ch, int start, int length)
      throws org.xml.sax.SAXException {
    // -- do nothing
  }

  public void processingInstruction(String target, String data) throws org.xml.sax.SAXException {
    // -- do nothing
  }

  public void setDocumentLocator(Locator locator) {
    componentReader.setDocumentLocator(locator);
  }

  public void startDocument() throws org.xml.sax.SAXException {
    // -- do nothing
  }

  public void startElement(String name, AttributeList atts) throws org.xml.sax.SAXException {
    // -- create new Namespace scope
    namespacesStack.addNewNamespaceScope();

    // -- handle namespaces
    AttributeSet attSet = processAttributeList(atts);

    String namespace = null;
    int idx = name.indexOf(':');
    if (idx >= 0) {
      String prefix = name.substring(0, idx);
      name = name.substring(idx + 1);
      namespace = namespacesStack.getNamespaceURI(prefix);
    } else {
      namespace = namespacesStack.getNamespaceURI("");
    }

    try {
      componentReader.startElement(name, namespace, attSet,
          namespacesStack.getCurrentNamespaceScope());
    } catch (XMLException ex) {
      throw new SAXException(ex);
    }
  }

  public void error(SAXParseException exception) throws org.xml.sax.SAXException {
    String systemId = exception.getSystemId();
    String err = "Parsing Error : " + exception.getMessage() + '\n' + "Line : "
        + exception.getLineNumber() + '\n' + "Column : " + exception.getColumnNumber() + '\n';
    if (systemId != null) {
      err = "In document: '" + systemId + "'\n" + err;
    }

    throw new SAXException(err);
  }

  public void fatalError(SAXParseException exception) throws org.xml.sax.SAXException {
    String systemId = exception.getSystemId();
    String err = "Parsing Error : " + exception.getMessage() + '\n' + "Line : "
        + exception.getLineNumber() + '\n' + "Column : " + exception.getColumnNumber() + '\n';
    if (systemId != null) {
      err = "In document: '" + systemId + "'\n" + err;
    }
    throw new SAXException(err);
  }

  public void warning(SAXParseException exception) throws org.xml.sax.SAXException {
    String systemId = exception.getSystemId();
    String err = "Parsing Error : " + exception.getMessage() + '\n' + "Line : "
        + exception.getLineNumber() + '\n' + "Column : " + exception.getColumnNumber() + '\n';
    if (systemId != null) {
      err = "In document: '" + systemId + "'\n" + err;
    }
    throw new SAXException(err);
  }

}
