package gov.nasa.jpf.shadow;

import gov.nasa.jpf.symbc.numeric.PathCondition;

public class MyPathCondition {

    public enum PathResultType {
        UNKNOWN, PROPERTY_VIOLATED, METHOD_END
    };

    public PathCondition pc;
    public PathResultType pathResultType = PathResultType.UNKNOWN;

    public MyPathCondition(PathCondition pc, PathResultType pathResultType) {
        this.pc = pc;
        this.pathResultType = pathResultType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MyPathCondition other = (MyPathCondition) obj;
        if (this.pc != null && other.pc == null) {
            return false;
        }
        if (this.pc == null && other.pc != null) {
            return false;
        }
        if (this.pc != null && other.pc != null) {
            if (!this.pc.equals(other.pc)) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pc == null) ? 0 : pc.hashCode());
        result = prime * result + ((pathResultType == null) ? 0 : pathResultType.hashCode());
        return result;
    }

}
