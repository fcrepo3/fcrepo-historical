// NMI's Java Code Viewer 5.1 � 1997-2001 B. Lemaire
// http://njcv.htmlplanet.com - info@njcv.htmlplanet.com

// Copy registered to Evaluation Copy                                   

// Source File Name:   Operation.java

package com.sun.xml.rpc.wsdl.document;

import com.sun.xml.rpc.wsdl.framework.Entity;
import com.sun.xml.rpc.wsdl.framework.EntityAction;
import java.util.*;
import javax.xml.rpc.namespace.QName;

// Referenced classes of package com.sun.xml.rpc.wsdl.document:
//            Fault, Input, OperationStyle, Output, 
//            WSDLConstants, WSDLDocumentVisitor, Documentation

public class Operation extends Entity {

    private Documentation _documentation;
    private String _name;
    private Input _input;
    private Output _output;
    private List _faults;
    private OperationStyle _style;
    private String _parameterOrder;
    private String _uniqueKey;

    public Operation() {
        _faults = new ArrayList();
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

    public Input getInput() {
        return _input;
    }

    public void setInput(Input i) {
        _input = i;
    }

    public Output getOutput() {
        return _output;
    }

    public void setOutput(Output o) {
        _output = o;
    }

    public void addFault(Fault f) {
        _faults.add(f);
    }

    public Iterator faults() {
        return _faults.iterator();
    }

    public String getParameterOrder() {
        return _parameterOrder;
    }

    public void setParameterOrder(String s) {
        _parameterOrder = s;
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

    public void withAllSubEntitiesDo(EntityAction action) {
        super.withAllSubEntitiesDo(action);
        if(_input != null)
            action.perform(_input);
        if(_output != null)
            action.perform(_output);
        for(Iterator iter = _faults.iterator(); iter.hasNext(); action.perform((Entity)iter.next()));
    }

    public void accept(WSDLDocumentVisitor visitor) throws Exception {
        visitor.preVisit(this);
        if(_input != null)
            _input.accept(visitor);
        if(_output != null)
            _output.accept(visitor);
        for(Iterator iter = _faults.iterator(); iter.hasNext(); ((Fault)iter.next()).accept(visitor));
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
            if(_parameterOrder != null)
                failValidation("validation.invalidAttribute", "parameterOrder");
        } else
        if(_style == OperationStyle.NOTIFICATION && _parameterOrder != null)
            failValidation("validation.invalidAttribute", "parameterOrder");
    }
}
