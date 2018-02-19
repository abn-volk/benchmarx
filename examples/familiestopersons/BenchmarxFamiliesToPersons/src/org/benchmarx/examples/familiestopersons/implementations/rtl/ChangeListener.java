package org.benchmarx.examples.familiestopersons.implementations.rtl;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.tzi.use.api.UseApiException;
import org.tzi.use.api.UseSystemApi;
import org.tzi.use.main.Session;
import org.tzi.use.uml.sys.MSystemState;

public class ChangeListener extends EContentAdapter {
	
	public static SimpleDateFormat compatibleFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private Session session;
	private MSystemState state;
	private PrintWriter logWriter;
	private UseSystemApi api;
	private Map<EObject, String> eObjToUse;
	
	public ChangeListener(Session session, MSystemState state, PrintWriter logWriter, UseSystemApi api,
			Map<EObject, String> eObjToUse) {
		super();
		this.session = session;
		this.state = state;
		this.logWriter = logWriter;
		this.api = api;
		this.eObjToUse = eObjToUse;
	}
	
	public void observe(EObject obj) {
		obj.eAdapters().add(this);
	}

	@Override
	public void notifyChanged(Notification noti) {
		super.notifyChanged(noti);
		if (noti.getEventType() == Notification.REMOVING_ADAPTER)
			return;
		//System.out.println(Integer.toString(noti.getEventType()));
		EObject affectedElement = (EObject) noti.getNotifier();
		EStructuralFeature feature = (EStructuralFeature) noti.getFeature();
		//System.out.println(affectedElement.toString());
		//System.out.println(feature.getName());
		String affectedObj = eObjToUse.get(affectedElement);
		if (affectedObj == null)
			affectedObj = createObject(affectedElement);
		if (affectedObj == null) {
			logWriter.println("Failed to create corresponding USE object for " + affectedElement.toString());
			return;
		}
		// EReference
		if (feature instanceof EReference) {
			String assocName = getUseAssoc(feature.getName());
			if (assocName == null) {
				logWriter.println("No corresponding USE association name found for the reference named " + feature.getName());
				return;
			}
			switch (noti.getEventType()) {
			case Notification.ADD:
			case Notification.SET:
				Object newValue = noti.getNewValue();
				if (newValue instanceof EObject) {
					String newObject = eObjToUse.get(newValue);
					if (newObject == null)
						newObject = createObject((EObject) newValue);
					if (newObject == null) {
						logWriter.println("Failed to create corresponding USE object for " + newValue.toString());
						return;
					}
					try {
						api.createLink(assocName, affectedObj, newObject);
					} catch (UseApiException e) {
						logWriter.println("Error when creating link: " + e.getMessage());
						e.printStackTrace();
					}
				}
				break;
			case Notification.REMOVE:
			case Notification.UNSET:
				Object oldValue = noti.getOldValue();
				String oldObject = eObjToUse.get(oldValue);
				if (oldObject != null) {
					try {
						//System.out.println("removing link");
						api.deleteLink(assocName, new String[] {affectedObj, oldObject});
					} catch (UseApiException e) {
						logWriter.println("Error when deleting link: " + e.getMessage());
						e.printStackTrace();
					}
				}
				break;
			default:
				logWriter.println("Event type not supported: " + Integer.toString(noti.getEventType()));
				break;
			}
			
			
		}
		// EAttribute
		else {
			// Is a single attribute. In the Fam2Per case, all attributes are of String/Date type
			if (feature.getUpperBound() != 1)
				return;
//			System.out.println("Changed " + feature.getName());
			Object value = noti.getNewValue();
//			System.out.println("Of: " + affectedObj);
//			System.out.println("To: " + value.toString());
			try {
				if ((affectedElement.eClass().getName().equals("Male") || affectedElement.eClass().getName().equals("Female")) && feature.getName().equals("name")) {
					if (value != null) {
						String[] name = ((String) value).split(", ");
						if (name.length != 2)
							logWriter.println("Wrong full name format: " + value.toString());
						else {
							api.setAttributeValue(affectedObj, "familyName", name[0]);
							api.setAttributeValue(affectedObj, "givenName", name[1]);
						}
					}
				}
				else {
					String valueRepr = (value == null)? "Undefined" : format(value);
					api.setAttributeValue(affectedObj, feature.getName(), valueRepr);
				}
			} catch (UseApiException e) {
				logWriter.println("Error when setting attribute: " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		
//		logWriter.println("Affected element: " + affectedElement.toString());
//		logWriter.println("Feature: " + feature.getName());
//		logWriter.println("Type: " + Integer.toString(noti.getEventType()));
//		if (noti.getOldValue() != null)
//			logWriter.println("Old value: " + noti.getOldValue().toString());
//		if (noti.getNewValue() != null)
//			logWriter.println("New value: " + noti.getNewValue().toString());
		
	}
	
	private String createObject(EObject affectedElement) {
		String className = affectedElement.eClass().getName();
		String objectName = state.uniqueObjectNameForClass(className);
		try {
			api.createObject(className, objectName);
			for (EAttribute attr : affectedElement.eClass().getEAllAttributes()) {
				String attrName = attr.getName();
				Object attrValue = affectedElement.eGet(attr);
				if ((affectedElement.eClass().getName().equals("Male") || affectedElement.eClass().getName().equals("Female")) && attrName.equals("name")) {
					if (attrValue != null) {
						String[] name = ((String) attrValue).split(", ");
						if (name.length != 2)
							logWriter.println("Wrong full name format: " + attrValue.toString());
						else {
							api.setAttributeValue(objectName, "familyName", "'" + name[0] + "'");
							api.setAttributeValue(objectName, "givenName", "'" + name[1] + "'");
						}
					}
				}
				else api.setAttributeValue(objectName, attrName, format(attrValue));
			}
		} catch (UseApiException e) {
			e.printStackTrace();
			logWriter.println("Error when creating new object " + objectName + " : " + className + ": " + e.getMessage());
		}
		eObjToUse.put(affectedElement, objectName);
		return objectName;
	}

	private String format(Object value) {
		if (value instanceof String)
			return "'" + value.toString() + "'";
		else if (value instanceof Date)
			return "'" + compatibleFormat.format((Date) value) + "'";
		else return value.toString();
			
	}
	
	private String getUseAssoc(String refName) {
		if (refName.equals("families"))
			return "FamilyRegistration";
		if (refName.equals("father"))
			return "Father";
		if (refName.equals("mother"))
			return "Mother";
		if (refName.equals("sons"))
			return "Sons";
		if (refName.equals("daughters"))
			return "Daughters";
		if (refName.equals("persons"))
			return "PersonRegistration";
		return null;
	}

}
