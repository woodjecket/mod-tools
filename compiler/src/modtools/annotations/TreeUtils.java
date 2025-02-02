package modtools.annotations;

import arc.struct.Seq;
import arc.util.Log;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import javax.lang.model.element.*;
import java.util.*;
import java.util.regex.Pattern;

import static modtools.annotations.BaseProcessor.*;

@SuppressWarnings("UnusedReturnValue")
public interface TreeUtils extends ParseUtils {
	/**
	 * 检测字段是否存在
	 *
	 * @return {@code true} if field is present.
	 */
	default boolean checkField(
	 JCClassDecl classDecl, String fieldName, String group
	) {
		JCVariableDecl fieldElem = findChild(classDecl, Tag.VARDEF, var -> var.name.toString().equals(fieldName));
		if (fieldElem == null) {
			Log.err(new IllegalStateException("You don't mark class (" + classDecl.getSimpleName() + ") or mark it group (" + group + ")." + "\n(debug)Needed (" + fieldName + ") field."));
			return false;
		}
		return true;
	}
	default JCVariableDecl addField(JCClassDecl classDecl, int flags, Type type, String name, String init) {
		JCVariableDecl x = makeVar(flags, type, name, init, classDecl.sym);
		x.pos = 0;
		classDecl.defs = classDecl.defs.prepend(x);
		return x;
	}
	default JCVariableDecl makeVar(long flags, Type type, String name, String init, Symbol sym) {
		return makeVar0(flags, type, name, init == null ? null : parseExpression(init), sym);
	}
	Pattern pattern = Pattern.compile("^[0-9a-zA-Z\\-$_ ]+$");
	default JCVariableDecl makeVar0(long flags, Type type, String name, JCExpression init, Symbol sym) {
		return makeVar1(flags, type, name, init, sym, true);
	}
	private JCVariableDecl makeVar1(long flags, Type type, String name, JCExpression init, Symbol sym, boolean enterScope) {
		if (!pattern.matcher(name).find())
			throw new IllegalArgumentException("Name(" + name + ") contains illegal char.");
		VarSymbol varSymbol = new VarSymbol(
		 flags, names.fromString(name), type, sym
		);
		if (enterScope && sym instanceof ClassSymbol csm) {
			csm.members_field.enter(varSymbol);
		}
		return mMaker.VarDef(varSymbol, init);
	}
	default JCVariableDecl addConstantField(JCClassDecl classDecl, Type type, String name, Object value) {
		JCVariableDecl x = makeVar0(Flags.STATIC | Flags.FINAL | Flags.HASINIT,
		 type, name, mMaker.Literal(value), classDecl.sym);
		classDecl.defs = classDecl.defs.prepend(x);
		return x;
	}

	default Type findTypeBoot(String name) {
		return mSymtab.getClass(mSymtab.java_base, names.fromString(name)).type;
	}
	default ClassType findType(String name) {
		return (ClassType) mSymtab.getClass(mSymtab.unnamedModule, names.fromString(name)).type;
	}
	default ClassSymbol findClassSymbolByBoot(String name) {
		return mSymtab.getClass(mSymtab.java_base, names.fromString(name));
	}

	default ClassSymbol findClassSymbol(String name) {
		return mSymtab.getClass(mSymtab.unnamedModule, names.fromString(name));
	}

	default void addImport(TypeElement element, ClassType classType) {
		addImport(element, (ClassSymbol) classType.tsym);
	}

	default void addImport(TypeElement element, ClassSymbol sym) {
		JCCompilationUnit unit = (JCCompilationUnit) trees.getPath(element).getCompilationUnit();
		if (!unit.namedImportScope.includes(sym) && !unit.starImportScope.includes(sym)) {
			/* unit.namedImportScope.importType(
			 SettingUI().members(), SettingUI().members(), SettingUI()
			); */
			unit.namedImportScope.importType(sym.owner.members(), sym.owner.members(), sym);
			Seq seq = Seq.with(unit.defs.toArray());
			seq.insert(1, mMaker.Import(mMaker.Select(mMaker.Ident(
			 sym.packge().fullname), sym), false));
			unit.defs = List.from(seq);
		}
	}
}
