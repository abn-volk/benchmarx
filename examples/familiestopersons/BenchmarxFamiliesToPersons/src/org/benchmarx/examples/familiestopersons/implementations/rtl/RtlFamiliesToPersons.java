package org.benchmarx.examples.familiestopersons.implementations.rtl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.benchmarx.Configurator;
import org.benchmarx.emf.BXToolForEMF;
import org.benchmarx.examples.familiestopersons.testsuite.Decisions;
import org.benchmarx.families.core.FamiliesComparator;
import org.benchmarx.persons.core.PersonsComparator;
import org.eclipse.emf.ecore.EObject;
import org.tzi.use.api.UseApiException;
import org.tzi.use.api.UseSystemApi;
import org.tzi.use.main.Session;
import org.tzi.use.uml.sys.MSystemState;
import org.uet.dse.rtlplus.Main;
import org.uet.dse.rtlplus.RTLLoader;
import org.uet.dse.rtlplus.matching.Match;
import org.uet.dse.rtlplus.sync.SyncWorker;

import com.google.common.eventbus.EventBus;

import Families.FamiliesFactory;
import Families.FamilyRegister;
import Persons.PersonRegister;
import Persons.PersonsFactory;

public class RtlFamiliesToPersons extends BXToolForEMF<FamilyRegister, PersonRegister, Decisions> {

	private Session session;
	private MSystemState state;
	private RTLLoader loader;
	private PrintWriter logWriter;
	private UseSystemApi api;
	private FamilyRegister famReg;
	private PersonRegister perReg;
	private Map<EObject, String> eObjToUse;
	private ChangeListener listener;
	private ModelConverter converter;
	private SyncWorker syncWorker;
	private EventBus eventBus;
	private Configurator<Decisions> configurator;
	
	public RtlFamiliesToPersons() {
		super(new FamiliesComparator(), new PersonsComparator());
	}
	
	@Override
	public void initiateSynchronisationDialogue() {
		// Create new USE session
		session = new Session();
		File familiesUse = new File("../implementationArtefacts/RTL/Families.use");
		File personsUse = new File("../implementationArtefacts/RTL/Persons.use");
		String tggName = "../implementationArtefacts/RTL/Families2Persons4.tgg";
		File logFile = new File("../implementationArtefacts/RTL/log.txt");
		configurator = new Configurator<Decisions>();
		configurator.makeDecision(Decisions.PREFER_CREATING_PARENT_TO_CHILD, true);
		configurator.makeDecision(Decisions.PREFER_EXISTING_FAMILY_TO_NEW, true);
		try {
			logWriter = new PrintWriter(logFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		loader = new RTLLoader(session, familiesUse, personsUse, tggName, logWriter);
		loader.run();
		state = session.system().state();
		api = UseSystemApi.create(session);
		converter = new ModelConverter(session, state, logWriter, api);
		syncWorker = new SyncWorker(null, logWriter, session);
		eventBus = session.system().getEventBus();
		try {
			api.createObject("FamilyRegister", "famReg");
			api.createObject("PersonRegister", "perReg");
			api.createObject("FR2PR", "fr2pr");
			api.createLink("FR2PR_FamilyRegister", new String[] {"famReg", "fr2pr"});
			api.createLink("FR2PR_PersonRegister", new String[] {"perReg", "fr2pr"});
			famReg = FamiliesFactory.eINSTANCE.createFamilyRegister();
			perReg = PersonsFactory.eINSTANCE.createPersonRegister();
			eObjToUse = new HashMap<>();
			eObjToUse.put(famReg, "famReg");
			eObjToUse.put(perReg, "perReg");
			listener = new ChangeListener(session, state, logWriter, api, eObjToUse);
			listener.observe(famReg);
			listener.observe(perReg);
			
		} catch (UseApiException e) {
			e.printStackTrace();
		}
		eventBus.register(syncWorker);
	}
	
	@Override
	public void performIdleSourceEdit(Consumer<FamilyRegister> edit) {
		sortRules();
		eventBus.unregister(syncWorker);
		edit.accept(getSourceModel());
		eventBus.register(syncWorker);
//		logWriter.println("\n\n\nIdle source edit");
//		logWriter.println("================= Families =====================");
//		logWriter.println(new FamiliesComparator().familyToString(famReg));
//		logWriter.println("================= USE =====================");
//		logWriter.println(state.allObjects().toString());
	}

	@Override
	public void performIdleTargetEdit(Consumer<PersonRegister> edit) {
		sortRules();
		eventBus.unregister(syncWorker);
		edit.accept(getTargetModel());
		eventBus.register(syncWorker);
//		logWriter.println("\n\n\nIdle target edit");
//		logWriter.println("================= Persons =====================");
//		logWriter.println(new PersonsComparator().personsToString(perReg));
//		logWriter.println("================= USE =====================");
//		logWriter.println(state.allObjects().toString());
	}

	@Override
	public void performAndPropagateSourceEdit(Consumer<FamilyRegister> edit) {
		sortRules();
		edit.accept(getSourceModel());
//		logWriter.println("\n\n\nSource edit");
//		logWriter.println("================= Families =====================");
//		logWriter.println(new FamiliesComparator().familyToString(famReg));
//		logWriter.println("================= USE =====================");
//		logWriter.println(state.allObjects().toString());
		// Invalidate person model
		perReg = null;
	}

	@Override
	public void performAndPropagateTargetEdit(Consumer<PersonRegister> edit) {
		sortRules();
		edit.accept(getTargetModel());
//		logWriter.println("\n\n\nTarget edit");
//		logWriter.println("================= Persons =====================");
//		logWriter.println(new PersonsComparator().personsToString(perReg));
//		logWriter.println("================= USE =====================");
//		logWriter.println(state.allObjects().toString());
		// Invalidate family model
		famReg = null;
		
	}

	@Override
	public void setConfigurator(Configurator<Decisions> conf) {
		configurator = conf;
	}
	
	@Override
	public void terminateSynchronisationDialogue() {
//		logWriter.println("\n\n\nFinished");
//		logWriter.println("================= Families =====================");
//		logWriter.println(new FamiliesComparator().familyToString(famReg));
//		logWriter.println("================= Persons =====================");
//		logWriter.println(new PersonsComparator().personsToString(perReg));
//		logWriter.println("================= USE =====================");
//		logWriter.println(state.allObjects().toString());
		super.terminateSynchronisationDialogue();
//		logWriter.println("END\n\n");
		logWriter.close();
	}

	@Override
	public String getName() {
		return "RTL";
	}

	@Override
	public FamilyRegister getSourceModel() {
		if (famReg == null) {
			famReg = converter.getFamilyRegister(eObjToUse);
			listener.observe(famReg);
		}
		return famReg;
	}

	@Override
	public PersonRegister getTargetModel() {
		if (perReg == null) {
			perReg = converter.getPersonRegister(eObjToUse);
			listener.observe(perReg);
		}
		return perReg;
	}

	@Override
	public void saveModels(String name) {
		
	}
	
	private void sortRules() {
		boolean parent = configurator.decide(Decisions.PREFER_CREATING_PARENT_TO_CHILD);
		boolean existing = configurator.decide(Decisions.PREFER_EXISTING_FAMILY_TO_NEW);
		Main.matchComparator = new Comparator<Match>() {
			@Override
			public int compare(Match arg0, Match arg1) {
				boolean arg0Parent = arg0.getRule().getName().contains("Father") || arg0.getRule().getName().contains("Mother");
				boolean arg1Parent = arg1.getRule().getName().contains("Father") || arg1.getRule().getName().contains("Mother");
				boolean arg0New = arg0.getRule().getName().contains("NewFamily");
				boolean arg1New = arg1.getRule().getName().contains("NewFamily");
				int arg0Point = 0;
				int arg1Point = 0;
				if (parent == arg0Parent) arg0Point--;
				if (existing == !arg0New) arg0Point--;
				if (parent == arg1Parent) arg1Point--;
				if (existing == !arg1New) arg1Point--;
				return Integer.compare(arg0Point, arg1Point);
			}
		};
	}
}
