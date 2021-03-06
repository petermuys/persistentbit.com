package com.persistbit.core.collections;

import com.persistentbit.core.Nullable;
import com.persistentbit.core.utils.ImTools;

import java.util.Date;
import java.util.Optional;

/**
 * User: petermuys
 * Date: 25/08/16
 * Time: 18:26
 */
@SuppressWarnings({"AssignmentToDateFieldFromParameter", "UseOfObsoleteDateTimeApi"})
public class CaseData1{

  private static final ImTools<CaseData1> im = ImTools.get(CaseData1.class);
  private final int    id;
  private final String name;
  @Nullable
  private final Date   birthDate;
  @Nullable
  private final String userName;


  public CaseData1(int id, String name, @Nullable Date birthDate, @Nullable String userName) {
	this.id = id;
	this.name = name;
	this.birthDate = birthDate;
	this.userName = userName;
	im.checkNullFields(this);
  }

  @Override
  public int hashCode() {
	return im.hashCodeAll(this);
  }

  @Override
  public boolean equals(Object obj) {
	return im.equalsAll(this, obj);
  }

  @Override
  public String toString() {
	return im.toStringAllIgnoreNulls(this);
  }

  public int getId() {
	return id;
  }

  public String getName() {
	return name;
  }

  public Optional<Date> getBirthDate() {
	return Optional.ofNullable(birthDate);
  }

  public Optional<String> getUserName() {
	return Optional.ofNullable(userName);
  }
}
