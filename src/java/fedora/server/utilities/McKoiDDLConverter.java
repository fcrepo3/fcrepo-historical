package fedora.server.utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * <p><b>Title:</b> McKoiDDLConverter.java</p>
 * <p><b>Description:</b> </p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class McKoiDDLConverter
        implements DDLConverter {

    public McKoiDDLConverter() {
    }

    public boolean supportsTableType() {
        return true;
    }

    public List getDDL(TableSpec spec) {
        StringBuffer out=new StringBuffer();
        StringBuffer end=new StringBuffer();
        out.append("CREATE TABLE " + spec.getName() + " (\n");
        Iterator csi=spec.columnSpecIterator();
        int csNum=0;
        while (csi.hasNext()) {
            if (csNum>0) {
                out.append(",\n");
            }
            csNum++;
            ColumnSpec cs=(ColumnSpec) csi.next();
            out.append("  ");
            out.append(cs.getName());
            out.append(' ');
            out.append(cs.getType());
            if (cs.isAutoIncremented()) {
                out.append(" default UNIQUEKEY('");
                out.append(spec.getName());
                out.append("')");
            }
            if (cs.getDefaultValue()!=null) {
                out.append(" default '");
                out.append(cs.getDefaultValue());
                out.append("'");
            }
            if (cs.isNotNull()) {
                out.append(" NOT NULL");
            }
            if (cs.isUnique()) {
                if (!end.toString().equals("")) {
                    end.append(",\n");
                }
                end.append("  UNIQUE (");
                end.append(cs.getName());
                end.append(")");
            }
            if (cs.getIndexName()!=null) {
                out.append(" INDEX_BLIST");
            }
            if (cs.getForeignTableName()!=null) {
                if (!end.toString().equals("")) {
                    end.append(",\n");
                }
                end.append("  FOREIGN KEY ");
                end.append(cs.getName());
                end.append(" (");
                end.append(cs.getName());
                end.append(") REFERENCES ");
                end.append(cs.getForeignTableName());
                end.append(" (");
                end.append(cs.getForeignColumnName());
                end.append(")");
                if (cs.getOnDeleteAction()!=null) {
                    end.append(" ON DELETE ");
                    end.append(cs.getOnDeleteAction());
                }
            }
        }
        if (spec.getPrimaryColumnName()!=null) {
            out.append(",\n  PRIMARY KEY (");
            out.append(spec.getPrimaryColumnName());
            out.append(")");
        }
        if (!end.toString().equals("")) {
            out.append(",\n");
            out.append(end);
        }
        out.append("\n");
        out.append(")");
        ArrayList l=new ArrayList();
        l.add(out.toString());
        return l;
    }

}

