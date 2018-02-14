package org.benchmarx.examples.familiestopersons.implementations.rtl;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.List;

import org.tzi.use.api.UseSystemApi;
import org.tzi.use.main.Session;
import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.mm.MAssociationEnd;
import org.tzi.use.uml.ocl.value.StringValue;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MObjectState;
import org.tzi.use.uml.sys.MSystemState;

import Families.FamiliesFactory;
import Families.Family;
import Families.FamilyMember;
import Families.FamilyRegister;
import Persons.Person;
import Persons.PersonRegister;
import Persons.PersonsFactory;

public class ModelConverter {
	
	private Session session;
	private MSystemState state;
	private PrintWriter logWriter;
	private UseSystemApi api;
	
	private MAssociationEnd famRegFamReg;
	private MAssociationEnd famRegFam;
	private MAssociationEnd fatherFam;
	private MAssociationEnd fatherFat;
	private MAssociationEnd motherFam;
	private MAssociationEnd motherMot;
	private MAssociationEnd sonFam;
	private MAssociationEnd sonSons;
	private MAssociationEnd daughterFam;
	private MAssociationEnd daughterDaus;
	
	private MAssociationEnd perRegPerReg;
	private MAssociationEnd perRegPer;
	
	private FamiliesFactory famFactory = FamiliesFactory.eINSTANCE;
	private PersonsFactory perFactory = PersonsFactory.eINSTANCE;
	
	public ModelConverter(Session session, MSystemState state, PrintWriter logWriter, UseSystemApi api) {
		super();
		this.session = session;
		this.state = state;
		this.logWriter = logWriter;
		this.api = api;
		prepare();
	}
	
	public void prepare() {
		MAssociation famRegAss = state.system().model().getAssociation("FamilyRegistration");
		List<MAssociationEnd> famRegAssEnds = famRegAss.associationEnds();
		famRegFamReg = famRegAssEnds.get(0);
		famRegFam = famRegAssEnds.get(1);
		MAssociation father = state.system().model().getAssociation("Father");
		List<MAssociationEnd> fatAssEnds = father.associationEnds();
		fatherFam = fatAssEnds.get(0);
		fatherFat = fatAssEnds.get(1);
		MAssociation mother = state.system().model().getAssociation("Mother");
		List<MAssociationEnd> motAssEnds = mother.associationEnds();
		motherFam = motAssEnds.get(0);
		motherMot = motAssEnds.get(1);
		MAssociation sons = state.system().model().getAssociation("Sons");
		List<MAssociationEnd> sonAssEnds = sons.associationEnds();
		sonFam = sonAssEnds.get(0);
		sonSons = sonAssEnds.get(1);
		MAssociation daughters = state.system().model().getAssociation("Daughters");
		List<MAssociationEnd> dauAssEnds = daughters.associationEnds();
		daughterFam = dauAssEnds.get(0);
		daughterDaus = dauAssEnds.get(1);
		
		MAssociation perRegAss = state.system().model().getAssociation("PersonRegistration");
		List<MAssociationEnd> perRegAssEnds = perRegAss.associationEnds();
		perRegPerReg = perRegAssEnds.get(0);
		perRegPer = perRegAssEnds.get(1);
	}

	public FamilyRegister getFamilyRegister() {
		FamilyRegister famReg = famFactory.createFamilyRegister();
		MObject famRegObj = state.objectByName("famReg");
		if (famRegObj == null) {
			logWriter.println("Family register object 'famReg' not found in USE");
			return famReg;
		}
		List<MObject> families = famRegObj.getNavigableObjects(state, famRegFamReg, famRegFam, null);
		for (MObject familyObj : families) {
			MObjectState famState = familyObj.state(state);
			StringValue famName = (StringValue) famState.attributeValue("name");
			String familyName = famName.value();
			Family family = famFactory.createFamily();
			family.setName(familyName);
			famReg.getFamilies().add(family);
			List<MObject> fats = familyObj.getNavigableObjects(state, fatherFam, fatherFat, null);
			for (MObject fatherObj : fats) {
				FamilyMember father = famFactory.createFamilyMember();
				family.setFather(father);
				Value fatherNameValue = fatherObj.state(state).attributeValue("name");
				if (fatherNameValue.isDefined()) {
					String fatherName = ((StringValue) fatherNameValue).value();
					father.setName(fatherName);
				}
			}
			List<MObject> mots = familyObj.getNavigableObjects(state, motherFam, motherMot, null);
			for (MObject motherObj : mots) {
				FamilyMember mother = famFactory.createFamilyMember();
				family.setMother(mother);
				Value motherNameValue = motherObj.state(state).attributeValue("name");
				if (motherNameValue.isDefined()) {
					String motherName = ((StringValue) motherNameValue).value();
					mother.setName(motherName);
				}
			}
			List<MObject> sons = familyObj.getNavigableObjects(state, sonFam, sonSons, null);
			for (MObject sonObj : sons) {
				FamilyMember son = famFactory.createFamilyMember();
				family.getSons().add(son);
				Value sonNameValue = sonObj.state(state).attributeValue("name");
				if (sonNameValue.isDefined()) {
					String sonName = ((StringValue) sonNameValue).value();
					son.setName(sonName);
				}
			}
			List<MObject> daughters = familyObj.getNavigableObjects(state, daughterFam, daughterDaus, null);
			for (MObject daughterObj : daughters) {
				FamilyMember daughter = famFactory.createFamilyMember();
				family.getDaughters().add(daughter);
				Value daughterNameValue = daughterObj.state(state).attributeValue("name");
				if (daughterNameValue.isDefined()) {
					String daughterName = ((StringValue) daughterNameValue).value();
					daughter.setName(daughterName);
				}
			}
		}
		return famReg;
	}
	
	public PersonRegister getPersonRegister() {
		PersonRegister perReg = perFactory.createPersonRegister();
		MObject perRegObj = state.objectByName("perReg");
		if (perRegObj == null) {
			logWriter.println("Person register object 'perReg' not found in USE");
			return perReg;
		}
		for (MObject personObj : perRegObj.getNavigableObjects(state, perRegPerReg, perRegPer, null)) {
			Person person = (personObj.cls().name().equals("Male"))? perFactory.createMale() : perFactory.createFemale();
			perReg.getPersons().add(person);
			Value personNameValue = personObj.state(state).attributeValue("fullName");
			if (personNameValue.isDefined()) {
				String personName = ((StringValue) personNameValue).value();
				person.setName(personName);
			}
			Value personBirthdayValue = personObj.state(state).attributeValue("birthday");
			if (personBirthdayValue.isDefined()) {
				String personBirthday = ((StringValue) personBirthdayValue).value();
				try {
					person.setBirthday(ChangeListener.compatibleFormat.parse(personBirthday));
				} catch (ParseException e) {
					logWriter.println("Error parsing birthday from string: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		return perReg;
	}
}
