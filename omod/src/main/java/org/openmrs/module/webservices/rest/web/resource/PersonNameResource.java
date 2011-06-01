/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.webservices.rest.web.resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.annotation.SubResource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubResource;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * {@link Resource} for PersonNames, supporting standard CRUD operations
 */
@SubResource(parent = PersonResource.class, path = "names")
@Handler(supports = PersonName.class, order = 0)
public class PersonNameResource extends DelegatingSubResource<PersonName, Person, PersonResource> {
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("givenName");
			description.addProperty("middleName");
			description.addProperty("familyName");
			description.addProperty("familyName2");
			description.addSelfLink();
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			return description;
		} else if (rep instanceof FullRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("givenName");
			description.addProperty("middleName");
			description.addProperty("familyName");
			description.addProperty("familyName2");
			description.addProperty("preferred");
			description.addProperty("prefix");
			description.addProperty("familyNamePrefix");
			description.addProperty("familyNameSuffix");
			description.addProperty("degree");
			description.addProperty("auditInfo", findMethod("getAuditInfo"));
			description.addSelfLink();
			return description;
		}
		return null;
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubResource#getParent(java.lang.Object)
	 */
	@Override
	public Person getParent(PersonName instance) {
		return instance.getPerson();
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubResource#setParent(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	public void setParent(PersonName instance, Person person) {
		instance.setPerson(person);
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.api.SubResource#doGetAll(java.lang.Object,
	 *      org.openmrs.module.webservices.rest.web.RequestContext)
	 */
	@Override
	public List<PersonName> doGetAll(Person parent, RequestContext context) throws ResponseException {
		List<PersonName> names = new ArrayList<PersonName>();
		
		if (parent != null) {
			names.addAll(parent.getNames());
		}
		
		return names;
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#getByUniqueId(java.lang.String)
	 */
	@Override
	public PersonName getByUniqueId(String uuid) {
		return Context.getPersonService().getPersonNameByUuid(uuid);
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#delete(java.lang.Object, java.lang.String, org.openmrs.module.webservices.rest.web.RequestContext)
	 */
	@Override
	public void delete(PersonName pn, String reason, RequestContext context) throws ResponseException {
		pn.setVoided(true);
		pn.setVoidedBy(Context.getAuthenticatedUser());
		pn.setVoidReason(reason);
		pn.setDateVoided(new Date());
		Context.getPersonService().savePerson(pn.getPerson());
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#purge(java.lang.Object, org.openmrs.module.webservices.rest.web.RequestContext)
	 */
	@Override
	public void purge(PersonName pn, RequestContext context) throws ResponseException {
		pn.getPerson().removeName(pn);
		Context.getPersonService().savePerson(pn.getPerson());
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#save(java.lang.Object)
	 */
	@Override
	protected PersonName save(PersonName newName) {
		// make sure that the name has actually been added to the person
		boolean needToAdd = true;
		for (PersonName pn : newName.getPerson().getNames()) {
			if (pn.equals(newName)) {
				needToAdd = false;
				break;
			}
		}
		if (needToAdd)
			newName.getPerson().addName(newName);
		Context.getPersonService().savePerson(newName.getPerson());
		return newName;
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#newDelegate()
	 */
	@Override
	protected PersonName newDelegate() {
		return new PersonName();
	}
	
	/**
	 * Gets the display string for a person name.
	 * 
	 * @param personName the person name object.
	 * @return the display string.
	 */
	public String getDisplayString(PersonName personName) {
		return personName.getFullName();
	}
	
	/**
	 * Gets extra book-keeping info, for the full representation
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public SimpleObject getAuditInfo(PersonName name) throws Exception {
		SimpleObject ret = new SimpleObject();
		ret.put("creator", ConversionUtil.getPropertyWithRepresentation(name, "creator", Representation.REF));
		ret.put("dateCreated", ConversionUtil.convertToRepresentation(name.getDateCreated(), Representation.DEFAULT));
		ret.put("voided", ConversionUtil.convertToRepresentation(name.isVoided(), Representation.DEFAULT));
		if (name.isVoided()) {
			ret.put("voidedBy", ConversionUtil.getPropertyWithRepresentation(name, "voidedBy", Representation.REF));
			ret.put("dateVoided", ConversionUtil.convertToRepresentation(name.getDateVoided(), Representation.DEFAULT));
			ret.put("voidReason", ConversionUtil.convertToRepresentation(name.getVoidReason(), Representation.DEFAULT));
		}
		return ret;
	}
}
