package modtools.annotations;

import arc.util.Reflect;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Attribute.Array;
import com.sun.tools.javac.code.Attribute.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import sun.reflect.annotation.*;

import javax.lang.model.element.Element;
import java.lang.Class;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static modtools.annotations.BaseProcessor.*;

public interface AnnotationUtils {
	Class<?> AnnotationInvocationHandler = classOrThrow("sun.reflect.annotation.AnnotationInvocationHandler");
	/** @param overwrite 是否重写注解的参数值 */
	default <T extends Annotation> T getAnnotationByTree(
	 Class<T> clazz, CompilationUnitTree unit, Tree tree, boolean overwrite) {
		return getAnnotation0(clazz, unit, tree, trees.getElement(trees.getPath(unit, tree)), overwrite);
	}
	private void overwrite(CompilationUnitTree unit, Tree tree) {
		// unit.getTypeDecls().forEach(t -> typeAnnotations.organizeTypeAnnotationsBodies((JCClassDecl) t));
		 JCModifiers mods = HopeReflect.get(tree, "mods");
		mods.annotations.forEach(ann -> {
			ann.args.forEach(arg -> {
				covertTreeToAttribute(unit, ann.attribute, (JCAssign) arg);
			});
		});
	}
	/* 标识，以免冲突 */
	class MyList<T> extends ArrayList<T> {
		public MyList(int initialCapacity) {super(initialCapacity);}
		public MyList() {}
		public MyList(Collection<? extends T> c) {super(c);}
	}
	private static void covertTreeToAttribute(Attribute attribute, Object value) {
		if (attribute instanceof Constant) HopeReflect.set(Constant.class, attribute, "value", value);
		else if (attribute instanceof Array) {
			Attribute[] values = ((Array) attribute).values;
			var         list   = ((MyList<?>) value);
			for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
				Attribute attribute1 = values[i];
				covertTreeToAttribute(attribute1, list.get(i));
			}
		}
	}
	private void covertTreeToAttribute(CompilationUnitTree unit, Compound parent, JCAssign assign) {
		Object    value     = treeToConstant(unit, assign.rhs);
		Attribute attribute = parent.member(((JCIdent) assign.lhs).name);
		// Log.info("attr: @, value: @", attribute, value);
		covertTreeToAttribute(attribute, value);
	}

	default <T extends Annotation> T getAnnotationByElement(
	 Class<T> clazz, Element el, boolean overwrite) {
		CompilationUnitTree unit = trees.getPath(el).getCompilationUnit();
		JCTree              tree = elements.getTree(el);
		return getAnnotation0(clazz, unit, tree, el, overwrite);
	}

	private <T extends Annotation> T
	getAnnotation0(Class<T> clazz, CompilationUnitTree unit,
								 Tree tree, Element el, boolean overwrite) {
		if (overwrite) overwrite(unit, tree);
		T ann = el.getAnnotation(clazz);
		if (ann == null) return null;
		InvocationHandler h = Reflect.get(Proxy.class, ann, "h");
		HashMap<String, Object> map = Reflect.get(AnnotationInvocationHandler,
		 h, "memberValues");
		map.replaceAll((k, v) ->
		 HopeReflect.mirrorTypes.isInstance(v) || HopeReflect.mirrorType.isInstance(v) ?
			HopeReflect.defineMirrorClass((ExceptionProxy) v)
			: v);
		return ann;
	}

	default Map<String, Object>
	genericAnnotation(Class<? extends Annotation> clazz,
										CompilationUnitTree unit,
										Tree node) {
		Map<String, Object> ann = AnnotationType.getInstance(clazz).memberDefaults();
		for (JCExpression arg : ((JCAnnotation) node).args) {
			JCExpression rhs   = ((JCAssign) arg).rhs;
			Object       value = treeToConstant(unit, rhs);
			if (value != null) ann.put(((JCAssign) arg).lhs.toString(), value);
		}
		// Log.info(ann);
		return ann;
	}
	default Object treeToConstant(CompilationUnitTree unit, JCTree node) {
		if (node instanceof JCLiteral) {
			return ((JCLiteral) node).value;
		} else if (node instanceof JCNewArray) {
			return ((JCNewArray) node).elems.stream().map(t -> treeToConstant(unit, t))
			 .collect(Collectors.toCollection(MyList::new));
		} else if (node instanceof JCFieldAccess field) {
			TreePath path   = trees.getPath(unit, node);
			Symbol   symbol = trees.getElement(path);

			if (symbol instanceof VarSymbol v) {
				if ("class".equals(symbol.name.toString())) {
					return symbol.type.getTypeArguments().get(0);
				}
				if (field.selected instanceof JCIdent ident && ident.sym != symbol && ident.sym instanceof ClassSymbol cs) {
					JCVariableDecl child = findChild(trees.getTree(cs), Tag.VARDEF,
					 (JCVariableDecl t) -> t.name.equals(((JCFieldAccess) node).name));
					// if (child == null) return null;
					return treeToConstant(unit, child.init);
				}
				JCTree.JCVariableDecl decl = (JCTree.JCVariableDecl) trees.getTree(v);
				if (decl != null) {
					return treeToConstant(unit, decl.init);
				}
			}
		} else if (node instanceof JCIdent) {
			TreePath path   = trees.getPath(unit, node);
			Symbol   symbol = trees.getElement(path);
			if (symbol instanceof VarSymbol v) {
				JCTree.JCVariableDecl decl = (JCTree.JCVariableDecl) trees.getTree(v);
				if (decl != null) {
					return treeToConstant(unit, decl.init);
				}
			}
		}
		return null;
	}
}
