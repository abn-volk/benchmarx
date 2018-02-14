package org.benchmarx.examples.familiestopersons.implementations.rtl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
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
import org.uet.dse.rtlplus.RTLLoader;
import org.uet.dse.rtlplus.matching.ForwardMatchManager;
import org.uet.dse.rtlplus.matching.Match;
import org.uet.dse.rtlplus.matching.MatchManager;

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
	
	public RtlFamiliesToPersons() {
		super(new FamiliesComparator(), new PersonsComparator());
	}
	
	@Override
	public void initiateSynchronisationDialogue() {
		// Create new USE session
		session = new Session();
		File familiesUse = new File("/home/pnh/NCKH/use-5.0.0/Families2Persons/Families.use");
		File personsUse = new File("/home/pnh/NCKH/use-5.0.0/Families2Persons/Persons.use");
		String tggName = "/home/pnh/NCKH/use-5.0.0/Families2Persons/Families2Persons.tgg";
		File logFile = new File("/home/pnh/NCKH/use-5.0.0/Families2Persons/log.txt");
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
		try {
			api.createObject("FamilyRegister", "famReg");
			api.createObject("PersonRegister", "perReg");
			api.createObject("FR2PR", "fr2pr");
			api.createLink("FamilyRegister_FR2PR", new String[] {"famReg", "fr2pr"});
			api.createLink("PersonRegister_FR2PR", new String[] {"perReg", "fr2pr"});
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
	}
	
	@Override
	public void performIdleSourceEdit(Consumer<FamilyRegister> edit) {
		edit.accept(getSourceModel());
	}

	@Override
	public void performIdleTargetEdit(Consumer<PersonRegister> edit) {
		edit.accept(getTargetModel());
	}

	@Override
	public void performAndPropagateSourceEdit(Consumer<FamilyRegister> edit) {
		performIdleSourceEdit(edit);
		MatchManager manager = new ForwardMatchManager(state, false);
		List<Match> matches;
		do {
			matches = manager.findMatches();
			for (Match match : matches) {
				if (match.run(state, logWriter)) 
					continue;
			}
		}
		while (matches.size() > 0);
		// Invalidate person model
		perReg = null;
	}

	@Override
	public void performAndPropagateTargetEdit(Consumer<PersonRegister> edit) {
		// TODO Auto-generated method stub
		// Invalidate family model
		famReg = null;
		
	}

	@Override
	public void setConfigurator(Configurator<Decisions> conf) {
		// TODO Auto-generated method stub
	
	}
	
	@Override
	public void terminateSynchronisationDialogue() {
		super.terminateSynchronisationDialogue();
		logWriter.println("END\n\n");
		logWriter.close();
	}

	@Override
	public String getName() {
		return "RTL";
	}

	@Override
	public FamilyRegister getSourceModel() {
		if (famReg == null)
			famReg = converter.getFamilyRegister();
		return famReg;
	}

	@Override
	public PersonRegister getTargetModel() {
		if (perReg == null)
			perReg = converter.getPersonRegister();
		return perReg;
	}

	@Override
	public void saveModels(String name) {
		
	}

}
