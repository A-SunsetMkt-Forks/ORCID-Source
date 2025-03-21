package org.orcid.persistence.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * <p>
 * Entity to represent the other names a user may wish to store. This is used
 * extensively to attempt to identify works that may be affiliated to this user
 * 
 * orcid-entities - Dec 6, 2011 - OtherNameEntity
 * 
 * @author Declan Newman (declan)
 **/
@Entity
@Table(name = "other_name")
public class OtherNameEntity extends SourceAwareEntity<Long> implements Comparable<OtherNameEntity>, OrcidAware, DisplayIndexInterface {

    private static final long serialVersionUID = -3227122865862310024L;

    private Long id;
    private String displayName;
    private String orcid;    
    private String visibility;
    private Long displayIndex;

    /**
     * @return the id of the other_name
     */
    @Id
    @Column(name = "other_name_id")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "other_name_seq")
    @SequenceGenerator(name = "other_name_seq", sequenceName = "other_name_seq", allocationSize = 1)
    public Long getId() {
        return id;
    }

    /**
     * @param id
     *            the id of the other_name
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the displayName
     */
    @Column(name = "display_name")
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName
     *            the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Column(name = "orcid", nullable = false, updatable = false)
    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
    
    @Column
    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    @Column(name = "display_index")
    public Long getDisplayIndex() {
        return displayIndex;
    }

    public void setDisplayIndex(Long displayIndex) {
        this.displayIndex = displayIndex;
    }
    
    @Override
    public int compareTo(OtherNameEntity otherNameEntity) {
        if (displayName != null && otherNameEntity != null) {
            return displayName.compareTo(otherNameEntity.getDisplayName());
        } else {
            return 0;
        }
    }    
}
