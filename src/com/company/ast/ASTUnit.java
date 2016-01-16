package com.company.ast;

import com.company.ast.objects.ASTClass;
import com.company.ast.objects.ASTMethod;
import com.company.ast.objects.ASTVariable;

import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class ASTUnit {
	
	private CompilationUnit compilationUnit;
	private TypeDeclaration typeDeclaration;


	private ASTClass unitClass;
	
	public ASTUnit(CompilationUnit cu) {
		compilationUnit = cu;
		typeDeclaration = ((TypeDeclaration) compilationUnit.types().get(0));
	}

	/**
	 * Permet de gérer les différents attributs de notre classe
	 */
	private void registerAttributes() {
		FieldDeclaration fields[] = typeDeclaration.getFields();
		for (FieldDeclaration field : fields) {
			String type = field.getType().toString();
			String name = ((VariableDeclarationFragment) field.fragments().get(0)).getName().toString();
			ASTClass klass = new ASTClass(type);
			ASTVariable att = new ASTVariable(name, klass);
			unitClass.addAttribute(att);
		}
	}

	/**
	 * Permet de gérer les différentes méthodes de notre classe
	 */
	private void registerMethods() {
		MethodDeclaration methods[] = typeDeclaration.getMethods();

		for (MethodDeclaration method : methods) {

			ASTMethod md = new ASTMethod(method.getName().toString(), unitClass);

			Type typeReturn = method.getReturnType2();

			if(typeReturn != null) {

				md.setReturnType(new ASTClass(typeReturn.toString()));

				addParamOfMethod(method, md);

				Block block = method.getBody();
				MethodInvocationVisitor miv = new MethodInvocationVisitor();
				block.accept(miv);

				registerLocalVariables(md, miv);

				registerCalledMethods(md, miv);

				unitClass.addMethod(md);
			}
		}
	}

	/**
	 *
	 * @param method
	 * @param md
	 */
	private void addParamOfMethod(MethodDeclaration method, ASTMethod md) {
		for (Object param : method.parameters()) {
			VariableDeclaration variableDeclaration = (VariableDeclaration) param;
			String type = variableDeclaration.getStructuralProperty(SingleVariableDeclaration.TYPE_PROPERTY).toString();
			ASTVariable var = new ASTVariable(variableDeclaration.getName().toString(), new ASTClass(type));
			md.addParameter(var);
		}
	}
	
	private void registerLocalVariables(ASTMethod md, MethodInvocationVisitor miv) {

		List<VariableDeclarationStatement> variableDeclarations = miv.getVariableDeclarations();
		for (VariableDeclarationStatement var : variableDeclarations) {
			String name = ((VariableDeclarationFragment) var.fragments().get(0)).getName().toString();
			String type = var.getType().toString();
			ASTVariable local = new ASTVariable(name, new ASTClass(type));
			md.addLocalVariable(local);
		}

	}
	
	private void registerCalledMethods(ASTMethod md, MethodInvocationVisitor miv) {

		List<MethodInvocation> listmi = miv.getMethods();
		for (MethodInvocation methodBody : listmi) {

			String varName = methodBody.getExpression().toString();
			String methodName = methodBody.getName().toString();

			ASTMethod m;

			if("".equals(varName))
				varName = "this";

			if("this".equals(varName)) // s'il c'est un appelle de méthode de la classe on peut alors identifier leur ASTClass
				m = new ASTMethod(methodName, new ASTClass(md.getContainerClass().getName()));
			else
				m = new ASTMethod(methodName, new ASTClass(""));

			addParamOfCalledMethod(methodBody.arguments(), m);

			try {
				md.addCalledMethod(varName, m);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void addParamOfCalledMethod (List arguments, ASTMethod m) {
		for (Object arg : arguments) {
			ASTVariable param = new ASTVariable(arg.toString(), new ASTClass(""));
			m.addParameter(param);
		}
	}
	
	public void initializeClass() {
		unitClass = new ASTClass(typeDeclaration.getName().toString());
		registerAttributes();
		registerMethods();
	}

	public ASTClass getUnitClass() {
		return unitClass;
	}
}
