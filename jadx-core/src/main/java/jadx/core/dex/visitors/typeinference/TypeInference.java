package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;

import java.util.List;

public class TypeInference extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		for (SSAVar var : mth.getSVars()) {
			// inference variable type
			ArgType type = processType(var);
			if (type == null) {
				type = ArgType.UNKNOWN;
			}
			var.setType(type);

			// search variable name
			String name = processVarName(var);
			if (name != null) {
				var.setName(name);
			}
		}

		// fix type for vars used only in Phi nodes
		for (SSAVar sVar : mth.getSVars()) {
			if (sVar.isUsedInPhi()) {
				processPhiNode(sVar.getUsedInPhi());
			}
		}
	}

	private static ArgType processType(SSAVar var) {
		RegisterArg assign = var.getAssign();
		List<RegisterArg> useList = var.getUseList();
		if (assign != null && (useList.isEmpty() || var.isTypeImmutable())) {
			return assign.getType();
		}
		ArgType type = assign != null ? assign.getType() : ArgType.UNKNOWN;
		for (RegisterArg arg : useList) {
			ArgType useType = arg.getType();
			ArgType newType = ArgType.merge(type, useType);
			if (newType != null) {
				type = newType;
			}
		}
		return type;
	}

	private static void processPhiNode(PhiInsn phi) {
		ArgType type = phi.getResult().getType();
		if (!type.isTypeKnown()) {
			for (InsnArg arg : phi.getArguments()) {
				if (arg.getType().isTypeKnown()) {
					type = arg.getType();
					break;
				}
			}
		}
		phi.getResult().setType(type);
		for (int i = 0; i < phi.getArgsCount(); i++) {
			RegisterArg arg = phi.getArg(i);
			arg.setType(type);
			arg.getSVar().setName(phi.getResult().getName());
		}
	}

	private static String processVarName(SSAVar var) {
		String name = null;
		if (var.getAssign() != null) {
			name = var.getAssign().getName();
		}
		if (name != null) {
			return name;
		}
		for (RegisterArg arg : var.getUseList()) {
			String vName = arg.getName();
			if (vName != null) {
				name = vName;
			}
		}
		return name;
	}
}