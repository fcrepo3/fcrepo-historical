// NMI's Java Code Viewer 5.1 � 1997-2001 B. Lemaire
// http://njcv.htmlplanet.com - info@njcv.htmlplanet.com

// Copy registered to Evaluation Copy                                   

// Source File Name:   BindingOperation.java

package com.sun.xml.rpc.wsdl.document;

import com.sun.xml.rpc.wsdl.framework.*;
import java.util.*;
import javax.xml.rpc.namespace.QName;

// Referenced classes of package com.sun.xml.rpc.wsdl.document:
//            BindingFault, BindingInput, OperationStyle, BindingOutput, 
//            WSDLConstants, WSDLDocumentVisitor, Documentation

public class BindingOperation extends Entity
    implements Extensible {

    private ExtensibilityHelper _helper;
    private Documentation _documentation;
    private String _name;
    private BindingInput _input;
    private BindingOutput _output;
    private List _faults;
    private OperationStyle _style;
    private String _uniqueKey;

    public BindingOperation() {
        _faults = new ArrayList();
        _helper = new ExtensibilityHelper();
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getUniqueKey() {
        if(_uniqueKey == null) {
            StringBuffer sb = new StringBuffer();
            sb.append(_name);
            sb.append(' ');
            if(_input != null) {
                sb.append(_input.getName());
            } else {
                sb.append(_name);
                if(_style == OperationStyle.REQUEST_RESPONSE)
                    sb.append("Request");
                else
                if(_style == OperationStyle.SOLICIT_RESPONSE)
                    sb.append("Response");
            }
            sb.append(' ');
            if(_output != null) {
                sb.append(_output.getName());
            } else {
                sb.append(_name);
                if(_style == OperationStyle.SOLICIT_RESPONSE)
                    sb.append("Solicit");
                else
                if(_style == OperationStyle.REQUEST_RESPONSE)
                    sb.append("Response");
            }
            _uniqueKey = sb.toString();
        }
        return _uniqueKey;
    }

    public OperationStyle getStyle() {
        return _style;
    }

    public void setStyle(OperationStyle s) {
        _style = s;
    }

    public BindingInput getInput() {
        return _input;
    }

    public void setInput(BindingInput i) {
        _input = i;
    }

    public BindingOutput getOutput() {
        return _output;
    }

    public void setOutput(BindingOutput o) {
        _output = o;
    }

    public void addFault(BindingFault f) {
        _faults.add(f);
    }

    public Iterator faults() {
        return _faults.iterator();
    }

    public QName getElementName() {
        return WSDLConstants.QNAME_OPERATION;
    }

    public Documentation getDocumentation() {
        return _documentation;
    }

    public void setDocumentation(Documentation d) {
        _documentation = d;
    }

    public void addExtension(Extension e) {
        _helper.addExtension(e);
    }

    public Iterator extensions() {
        return _helper.extensions();
    }

    public void withAllSubEntitiesDo(EntityAction action) {
        if(_input != null)
            action.perform(_input);
        if(_output != null)
            action.perform(_output);
        for(Iterator iter = _faults.iterator(); iter.hasNext(); action.perform((Entity)iter.next()));
        _helper.withAllSubEntitiesDo(action);
    }

    public void accept(WSDLDocumentVisitor visitor) throws Exception {
        visitor.preVisit(this);
        if(_input != null)
            _input.accept(visitor);
        if(_output != null)
            _output.accept(visitor);
        for(Iterator iter = _faults.iterator(); iter.hasNext(); ((BindingFault)iter.next()).accept(visitor));
        _helper.accept(visitor);
        visitor.postVisit(this);
    }

    public void validateThis() {
        if(_name == null)
            failValidation("validation.missingRequiredAttribute", "name");
        if(_style == null)
            failValidation("validation.missingRequiredProperty", "style");
        if(_style == OperationStyle.ONE_WAY) {
            if(_input == null)
                failValidation("validation.missingRequiredSubEntity", "input");
            if(_output != null)
                failValidation("validation.invalidSubEntity", "output");
            if(_faults != null && _faults.size() != 0)
                failValidation("validation.invalidSubEntity", "fault");
        }
    }
}
