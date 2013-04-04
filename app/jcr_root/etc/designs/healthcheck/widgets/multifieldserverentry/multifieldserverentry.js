/**
 * @class CQ.Ext.CustomPathFieldWidget
 * @extends CQ.form.CompositeField
 * This is a custom path field with a Link Text and a Link URL
 * @param {Object} config the config object
 */
/**
 * @class Ejst.CustomWidget
 * @extends CQ.form.CompositeField This is a custom widget based on
 *          {@link CQ.form.CompositeField}.
 * @constructor Creates a new CustomWidget.
 * @param {Object}
 *            config The config object
 */
CQ.Ext.CustomPathFieldWidget = CQ.Ext.extend(CQ.form.CompositeField, {

	/**
	 * @private
	 * @type CQ.Ext.form.TextField
	 */
	hiddenField : null,

	/**
	 * @private
	 * @type CQ.Ext.form.TextField
	 */
	serverText : null,

	/**
	 * @private
	 * @type CQ.Ext.form.CheckBox
	 */
	enabledServer : null,

	/**
	 * @private
	 * @type CQ.Ext.form.FormPanel
	 */
	formPanel : null,

	constructor : function(config) {
		config = config || {};
		var defaults = {
			"border" : true,
			"labelWidth" : 75,
			"layout" : "form"
		// ”columns”:6
		};
		config = CQ.Util.applyDefaults(config, defaults);
		CQ.Ext.CustomPathFieldWidget.superclass.constructor.call(this,
				config);
	},

	// overriding CQ.Ext.Component#initComponent
	initComponent : function() {
		CQ.Ext.CustomPathFieldWidget.superclass.initComponent.call(this);

		// Hidden field
		this.hiddenField = new CQ.Ext.form.Hidden({
			name : this.name
		});
		this.add(this.hiddenField);

		// Link text
		this.add(new CQ.Ext.form.Label({
			cls : "customwidget-label",
			text : "Server Host Name"
		}));
		this.serverText = new CQ.Ext.form.TextField({
			cls : "customwidget-1",
			fieldLabel : "Server Host: ",
			allowBlank : false,
			listeners : {
				change : {
					scope : this,
					fn : this.updateHidden
				}
			}
		});
		this.add(this.serverText);

		// enabled Server
		this.enabledServer = new CQ.Ext.form.Checkbox({
			cls : "customwidget-2",
			boxLabel : "Enable Server",
			listeners : {
				change : {
					scope : this,
					fn : this.updateHidden
				},
				check : {
					scope : this,
					fn : this.updateHidden
				}
			}
		});
		this.add(this.enabledServer);

	},

	processInit : function(path, record) {
		this.serverText.processInit(path, record);
		this.enabledServer.processInit(path, record);
	},

	setValue : function(value) {
		var serverEntry = JSON.parse(value);
		this.serverText.setValue(serverEntry.serverText);
		this.enabledServer.setValue(serverEntry.enabledServer);
		this.hiddenField.setValue(value);
	},

	getValue : function() {
		return this.getRawValue();
	},

	getRawValue : function() {
		var link = {
			"serverText" : this.serverText.getValue(),
			"enabledServer" : this.enabledServer.getValue()
		};
		return JSON.stringify(link);
	},

	updateHidden : function() {
		this.hiddenField.setValue(this.getValue());
	}
});

CQ.Ext.reg('multifieldserverentry', CQ.Ext.CustomPathFieldWidget);