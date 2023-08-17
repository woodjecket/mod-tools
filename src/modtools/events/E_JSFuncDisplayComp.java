package modtools.events;

import modtools.annotations.DataEnum;

@SuppressWarnings("unused")
@DataEnum
enum E_JSFuncDisplayComp implements E_DataInterface {
	modifier, type, value, buttons;

	/* public static final Data data = MySettings.D_JSFUNC_DISPLAY;
	public boolean enabled() {
		return data.getBool(name());
	} */
	static {
		for (E_DataInterface value : values()) {
			value.def(true);
		}
	}
}
