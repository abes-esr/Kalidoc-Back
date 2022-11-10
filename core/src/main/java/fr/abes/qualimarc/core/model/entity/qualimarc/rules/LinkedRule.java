package fr.abes.qualimarc.core.model.entity.qualimarc.rules;

import fr.abes.qualimarc.core.utils.BooleanOperateur;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Règle liée composée d'une règle simple et d'un opérateur pour pouvoir la conditionner avec la règle précédente d'une liste
 * L'ordre d'exécution des règles liées est géré par l'attribut position
 */
@Getter @Setter
@NoArgsConstructor
public class LinkedRule extends OtherRule {

    @OneToOne(cascade = CascadeType.ALL)
    @NotNull
    @JoinColumn(name = "RULE_ID")
    private SimpleRule rule;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "OPERATOR")
    private BooleanOperateur operateur;

    public LinkedRule(SimpleRule rule, BooleanOperateur operateur, ComplexRule complexRule, Integer position) {
        super(rule.getId(), position, complexRule);
        this.rule = rule;
        this.operateur = operateur;
    }

    @Override
    public String getZones() {
        return this.getRule().getZone();
    }
}
