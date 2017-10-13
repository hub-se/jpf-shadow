package gov.nasa.jpf.symbc.numeric;

/*
 * This class stores the Symbolic Expression and the Shadow (Symbolic) Expression of a variable
 */
public class DiffExpression {
	Expression shadow_expr;
	Expression sym_expr;
	
	public DiffExpression(Expression shadow_v, Expression sym_v){
		this.shadow_expr = shadow_v;
		this.sym_expr = sym_v;
	}
	
	public Expression getShadow(){
		return this.shadow_expr;
	}
	
	public Expression getSymbc(){
		return this.sym_expr;
	}
	
	public void setSym(Expression sym){
		this.shadow_expr = sym;
	}
	
	public void setShadow(Expression shadow){
		this.sym_expr = shadow;
	}
}
