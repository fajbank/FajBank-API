package br.com.faj.bank.customer;

import br.com.faj.bank.customer.data.CustomerRepository;
import br.com.faj.bank.customer.domain.FetchCustomerUseCase;
import br.com.faj.bank.customer.domain.UpdateCustomerFieldUseCase;
import br.com.faj.bank.customer.model.request.UpdateCustomerRequest;
import br.com.faj.bank.customer.model.response.CustomerResponse;
import br.com.faj.bank.timeline.domain.RegisterCustomerTimelineUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/v1/customer")
public class CustomerController {

    private final FetchCustomerUseCase fetchCustomerUseCase;
    private final UpdateCustomerFieldUseCase updateCustomerFieldUseCase;
    private final RegisterCustomerTimelineUseCase registerCustomerTimelineUseCase;
    private final CustomerRepository customerRepository;

    public CustomerController(
            RegisterCustomerTimelineUseCase registerCustomerTimelineUseCase,
            UpdateCustomerFieldUseCase updateCustomerFieldUseCase,
            FetchCustomerUseCase fetchCustomerUseCase,
            CustomerRepository customerRepository
    ) {
        this.registerCustomerTimelineUseCase = registerCustomerTimelineUseCase;
        this.updateCustomerFieldUseCase = updateCustomerFieldUseCase;
        this.fetchCustomerUseCase = fetchCustomerUseCase;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public ResponseEntity<CustomerResponse> fetchCustomer() {

        var data = fetchCustomerUseCase.fetch();

        if (data == null) {
            throw new SessionAuthenticationException("No customer found");
        }

        return CustomerResponse.responseOk(data);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CustomerResponse>> fetchAllCustomers() {
        var data = customerRepository.findAllByOrderByIdDesc();
        return ResponseEntity.ok(data.stream().map(CustomerResponse::toResponse).toList());
    }

    @PutMapping("/delete-customer/{customerId}")
    public ResponseEntity<?> deleteCustomer(
            @PathVariable("customerId") Long customerId
    ) {
        var customer = customerRepository.findById(customerId);

        if (customer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        customerRepository.delete(customer.get());
        // Precisa limpar a base depois, criar use case para fazer isso

        var response = new HashMap<String, String>()
                .put("message", "Customer deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> editField(
            @RequestBody UpdateCustomerRequest request
    ) {

        var data = updateCustomerFieldUseCase.update(request.fields());

        if (data == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        registerCustomerTimelineUseCase.registerUpdateProfile();

        return CustomerResponse.responseOk(data);
    }
}
